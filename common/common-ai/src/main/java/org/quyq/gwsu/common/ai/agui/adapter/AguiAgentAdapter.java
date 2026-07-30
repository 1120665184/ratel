/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quyq.gwsu.common.ai.agui.adapter;

import com.google.gson.Gson;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.*;
import io.agentscope.core.util.JsonException;
import io.agentscope.core.util.JsonUtils;
import io.agentscope.harness.agent.HarnessAgent;
import org.quyq.gwsu.common.ai.agui.converter.AguiMessageConverter;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.model.AguiMessage;
import org.quyq.gwsu.common.ai.agui.model.RunAgentInput;
import org.quyq.gwsu.common.ai.loop.AgentApprovalResolver;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * Adapter that bridges AgentScope agents to the AG-UI protocol.
 *
 * <p>This adapter converts AG-UI protocol inputs to AgentScope messages,
 * invokes the agent, and converts the streaming events back to AG-UI events.
 *
 * <p><b>Event Mapping:</b>
 * <ul>
 *   <li>AgentScope REASONING events → AG-UI TEXT_MESSAGE_* events (for TextBlock)</li>
 *   <li>AgentScope REASONING events → AG-UI REASONING_* events (for ThinkingBlock, when enabled)</li>
 *   <li>AgentScope TOOL_RESULT events → AG-UI TOOL_CALL_END events</li>
 *   <li>ToolUseBlock content → AG-UI TOOL_CALL_START events</li>
 * </ul>
 *
 * <p><b>Reasoning Support:</b>
 * <ul>
 *   <li>ThinkingBlock content is converted to REASONING_* events according to AG-UI Reasoning draft</li>
 *   <li>Reasoning output is disabled by default (enableReasoning=false) for backward compatibility</li>
 *   <li>Set enableReasoning=true in AguiAdapterConfig to enable reasoning events</li>
 * </ul>
 */
public class AguiAgentAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AguiAgentAdapter.class);
    private final Agent agent;
    private final AguiAdapterConfig config;
    private final AguiMessageConverter messageConverter;

    private final Gson gson = new Gson();

    /**
     * Creates a new AguiAgentAdapter.
     *
     * @param agent  The agent to adapt
     * @param config The adapter configuration
     */
    public AguiAgentAdapter(Agent agent, AguiAdapterConfig config) {
        this.agent = Objects.requireNonNull(agent, "agent cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.messageConverter = new AguiMessageConverter();
    }

    /**
     * Run the agent with AG-UI protocol input.
     *
     * <p>This method converts the input messages, invokes the agent's streaming API,
     * and emits AG-UI protocol events.
     *
     * @param input The AG-UI run input
     * @return A Flux of AG-UI events
     */
    public Flux<AguiEvent> run(RunAgentInput input, RuntimeContext runtimeContext) {
        String threadId = runtimeContext.getSessionId();
        String runId = input.runId();

        // 1. 检查 approval 消息
        ApprovalResult approvalResult = null;
        AguiMessage approvalMsg = extractApprovalMessage(input);
        if (approvalMsg != null) {
            approvalResult = parseApprovalResult(approvalMsg);
            input = removeApprovalMessage(input);
            logger.debug("Approval message found for thread {}: result={}", threadId, approvalResult.result());
        }

        // Track state for event conversion
        EventConversionState state = new EventConversionState(threadId, runId);

        // Normal flow: convert AG-UI messages to AgentScope messages
        List<Msg> msgs = messageConverter.toMsgList(input.messages());

        // 处理审批恢复信息
        buildMsgFromApproval(approvalResult, msgs, runtimeContext);
        Flux<AgentEvent> agentEvents = agentCallEvents(msgs, runtimeContext);
        if (agentEvents == null) {
            return Flux.error(new IllegalStateException(
                    "当前 AG-UI 仅支持 ReActAgent/HarnessAgent 的 v2 event 流，实际类型: " + agent.getClass().getName()));
        }


        return Flux.concat(
                        // Emit RUN_STARTED
                        Flux.just(new AguiEvent.RunStarted(threadId, runId)),
                        // Stream agent events and convert to AG-UI events
                        agentEvents.concatMapIterable(event -> convertAgentEvent(event, state)),
                        // Emit any pending end events and RUN_FINISHED
                        Flux.defer(() -> finishRun(state)))
                .onErrorResume(
                        error -> {
                            String errorMessage =
                                    error.getMessage() != null
                                            ? error.getMessage()
                                            : error.getClass().getSimpleName();
                            logger.error("", error);
                            return Flux.just(
                                    new AguiEvent.Raw(
                                            threadId, runId, Map.of("error", errorMessage)),
                                    new AguiEvent.RunFinished(threadId, runId));
                        });
    }

    private Flux<AgentEvent> agentCallEvents(List<Msg> msgs, RuntimeContext context) {
        if (agent instanceof HarnessAgent harnessAgent) {
            return harnessAgent.streamEvents(msgs, context);
        }
        if (agent instanceof ReActAgent reactAgent) {
            return reactAgent.streamEvents(msgs, context);
        }
        return null;
    }


    /**
     * 从输入消息中提取 approval 消息。
     * approval 消息的 role 为 "approval"，content 为 JSON 格式的审批结果。
     */
    private AguiMessage extractApprovalMessage(RunAgentInput input) {
        List<AguiMessage> messages = input.messages();
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        AguiMessage last = messages.getLast();
        if ("approval".equalsIgnoreCase(last.role())) {
            return last;
        }
        return null;
    }

    /**
     * 解析 approval 消息的 content，构建 ApprovalResult。
     */
    private ApprovalResult parseApprovalResult(AguiMessage approvalMsg) {
        String content = approvalMsg.textContent();
        if (content == null || content.isEmpty()) {
            logger.warn("Approval message has empty content, defaulting to REJECTED");
            return new ApprovalResult(ApprovalResult.ApprovalEnum.REJECTED, null);
        }

        return gson.fromJson(content, ApprovalResult.class);
    }

    /**
     * 从输入消息中移除 approval 消息，返回新的 RunAgentInput。
     * approval 消息不进入上下文历史，处理完后必须移除。
     */
    private RunAgentInput removeApprovalMessage(RunAgentInput input) {
        List<AguiMessage> messages = input.messages();
        List<AguiMessage> filtered = messages.stream()
                .filter(msg -> !"approval".equalsIgnoreCase(msg.role()))
                .toList();
        return RunAgentInput.builder()
                .threadId(input.threadId())
                .runId(input.runId())
                .messages(filtered)
                .tools(input.tools())
                .context(input.context())
                .forwardedProps(input.forwardedProps())
                .build();
    }


    private void buildMsgFromApproval(
            ApprovalResult approvalResult, List<Msg> msgs, RuntimeContext runtimeContext) {
        if (Objects.isNull(approvalResult)) {
            return;
        }
        List<ConfirmResult> confirmResults =
                AgentApprovalResolver.buildConfirmResults(
                        agent,
                        runtimeContext.getSessionId(),
                        runtimeContext.getUserId(),
                        approvalResult);
        if (CollectionUtils.isEmpty(confirmResults)) {
            return;
        }
        msgs.add(Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .textContent(approvalResult.result().name() + (
                        StringUtils.hasText(approvalResult.rejectReason()) ? ",拒绝原因：" + approvalResult.rejectReason() : ""
                ))
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, confirmResults))
                .build());
        logger.debug("Resuming agent after permission decision: approved={}", approvalResult.isApproved());
    }

    private List<AguiEvent> convertAgentEvent(AgentEvent event, EventConversionState state) {
        // logger.info("智能体事件：{}" , gson.toJson(event));
        List<AguiEvent> events = new ArrayList<>();

        if (Objects.nonNull(event.getSource())) {
            return events;
        }
        if (event instanceof TextBlockStartEvent textStart) {
            events.add(new AguiEvent.TextMessageStart(
                    state.threadId, state.runId, textStart.getReplyId(), "assistant"));
        } else if (event instanceof TextBlockDeltaEvent textDelta) {
            if (textDelta.getDelta() != null && !textDelta.getDelta().isEmpty()) {
                events.add(new AguiEvent.TextMessageContent(
                        state.threadId, state.runId, textDelta.getReplyId(), textDelta.getDelta()));
            }
        } else if (event instanceof TextBlockEndEvent textEnd) {
            events.add(new AguiEvent.TextMessageEnd(state.threadId, state.runId, textEnd.getReplyId()));
        } else if (event instanceof ThinkingBlockStartEvent thinkingStart) {
            if (!config.isEnableReasoning()) {
                return events;
            }
            events.add(new AguiEvent.ReasoningMessageStart(
                    state.threadId, state.runId, reasoningMessageId(thinkingStart.getReplyId()), "reasoning"));
        } else if (event instanceof ThinkingBlockDeltaEvent thinkingDelta) {
            if (!config.isEnableReasoning()) {
                return events;
            }
            if (thinkingDelta.getDelta() != null && !thinkingDelta.getDelta().isEmpty()) {
                events.add(new AguiEvent.ReasoningMessageContent(
                        state.threadId,
                        state.runId,
                        reasoningMessageId(thinkingDelta.getReplyId()),
                        thinkingDelta.getDelta()));
            }
        } else if (event instanceof ThinkingBlockEndEvent thinkingEnd) {
            if (!config.isEnableReasoning()) {
                return events;
            }
            events.add(new AguiEvent.ReasoningMessageEnd(
                    state.threadId, state.runId, reasoningMessageId(thinkingEnd.getReplyId())));
        } else if (event instanceof ToolCallStartEvent toolCallStart) {
            events.add(new AguiEvent.ToolCallStart(
                    state.threadId,
                    state.runId,
                    toolCallStart.getToolCallId(),
                    toolCallStart.getToolCallName()));
        } else if (event instanceof ToolCallDeltaEvent toolCallDelta) {
            if (config.isEmitToolCallArgs()
                    && toolCallDelta.getDelta() != null
                    && !toolCallDelta.getDelta().isEmpty()) {
                events.add(new AguiEvent.ToolCallArgs(
                        state.threadId,
                        state.runId,
                        toolCallDelta.getToolCallId(),
                        toolCallDelta.getDelta()));
            }
        } else if (event instanceof ToolCallEndEvent toolCallEnd) {
            events.add(new AguiEvent.ToolCallEnd(
                    state.threadId, state.runId, toolCallEnd.getToolCallId()));
        } else if (event instanceof ToolResultStartEvent toolResultStart) {
            state.startToolResult(toolResultStart.getToolCallId());
        } else if (event instanceof ToolResultTextDeltaEvent toolResultDelta) {
            state.appendToolResult(toolResultDelta.getToolCallId(), toolResultDelta.getDelta());
        } else if (event instanceof ToolResultDataDeltaEvent toolResultDataDelta) {
            state.appendToolResult(toolResultDataDelta.getToolCallId(), serializeContentBlock(toolResultDataDelta.getData()));
        } else if (event instanceof ToolResultEndEvent toolResultEnd) {
            String toolCallId = toolResultEnd.getToolCallId();
            events.add(new AguiEvent.ToolCallResult(
                    state.threadId,
                    state.runId,
                    toolCallId,
                    state.consumeToolResult(toolCallId),
                    "tool",
                    toolResultEnd.getReplyId()));
        } else if (event instanceof CustomEvent customEvent) {
            events.add(new AguiEvent.Custom(
                    state.threadId,
                    state.runId,
                    customEvent.getName(),
                    customEvent.getValue()));
        }

        return events;
    }

    private String reasoningMessageId(String replyId) {
        return replyId + "-reasoning";
    }

    private String serializeContentBlock(ContentBlock block) {
        if (block == null) {
            return null;
        }
        if (block instanceof TextBlock textBlock) {
            return textBlock.getText();
        }
        try {
            return JsonUtils.getJsonCodec().toJson(block);
        } catch (JsonException e) {
            return block.toString();
        }
    }

    /**
     * Finish the run by emitting any pending end events and RUN_FINISHED.
     *
     * @param state The conversion state
     * @return Flux of final events
     */
    private Flux<AguiEvent> finishRun(EventConversionState state) {
        List<AguiEvent> events = new ArrayList<>();

//        // End any messages that weren't properly ended
//        for (String messageId : state.getStartedMessages()) {
//            if (!state.hasEndedMessage(messageId)) {
//                events.add(new AguiEvent.TextMessageEnd(state.threadId, state.runId, messageId));
//            }
//        }
//
//        // End any tool calls that weren't properly ended
//        for (String toolCallId : state.getStartedToolCalls()) {
//            if (!state.hasEndedToolCall(toolCallId)) {
//                events.add(new AguiEvent.ToolCallEnd(state.threadId, state.runId, toolCallId));
//            }
//        }
//
//        // End any reasoning messages that weren't properly ended
//        for (String messageId : state.getStartedReasoningMessages()) {
//            if (!state.hasEndedReasoningMessage(messageId)) {
//                events.add(
//                        new AguiEvent.ReasoningMessageEnd(state.threadId, state.runId, messageId));
//            }
//        }
        // Emit RUN_FINISHED
        events.add(new AguiEvent.RunFinished(state.threadId, state.runId));

        return Flux.fromIterable(events);
    }

    /**
     * Extract text content from a tool result block.
     *
     * @param toolResult The tool result block
     * @return The text content, or null if not present
     */
    private String extractToolResultText(ToolResultBlock toolResult) {
        if (toolResult.getOutput() == null || toolResult.getOutput().isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (ContentBlock output : toolResult.getOutput()) {
            if (output instanceof TextBlock textBlock) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(textBlock.getText());
            }
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Serialize tool arguments to JSON string.
     *
     * @param input The tool input map
     * @return JSON string representation
     */
    private String serializeToolArgs(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return "{}";
        }
        try {
            return JsonUtils.getJsonCodec().toJson(input);
        } catch (JsonException e) {
            return "{}";
        }
    }

    /**
     * State tracker for event conversion.
     * Uses LinkedHashSet to preserve insertion order for proper event sequencing.
     */
    private static class EventConversionState {
        final String threadId;
        final String runId;
        private final Map<String, StringBuilder> toolResultBuffers = new HashMap<>();

        EventConversionState(String threadId, String runId) {
            this.threadId = threadId;
            this.runId = runId;
        }

        void startToolResult(String toolCallId) {
            toolResultBuffers.putIfAbsent(toolCallId, new StringBuilder());
        }

        void appendToolResult(String toolCallId, String delta) {
            if (toolCallId == null || delta == null || delta.isEmpty()) {
                return;
            }
            toolResultBuffers.computeIfAbsent(toolCallId, ignored -> new StringBuilder()).append(delta);
        }

        String consumeToolResult(String toolCallId) {
            StringBuilder builder = toolResultBuffers.remove(toolCallId);
            return builder == null || builder.isEmpty() ? null : builder.toString();
        }
    }
}
