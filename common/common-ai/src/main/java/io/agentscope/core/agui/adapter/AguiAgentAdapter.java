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
package io.agentscope.core.agui.adapter;

import cn.hutool.core.collection.CollUtil;
import com.google.gson.Gson;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.agui.converter.AguiMessageConverter;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.message.*;
import io.agentscope.core.util.JsonException;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalResult;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
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
    public Flux<AguiEvent> run(RunAgentInput input) {
        String threadId = input.getThreadId();
        String runId = input.getRunId();

        // 1. 检查 approval 消息
        ApprovalResult approvalResult = null;
        AguiMessage approvalMsg = extractApprovalMessage(input);
        if (approvalMsg != null) {
            approvalResult = parseApprovalResult(approvalMsg);
            input = removeApprovalMessage(input);
            logger.debug("Approval message found for thread {}: result={}", threadId, approvalResult.result());
        }

        // Create stream options - use incremental mode for true streaming
        StreamOptions options =
                StreamOptions.builder().eventTypes(EventType.ALL).incremental(true).build();

        // Track state for event conversion
        EventConversionState state = new EventConversionState(threadId, runId);

        // Normal flow: convert AG-UI messages to AgentScope messages
        List<Msg> msgs = messageConverter.toMsgList(input.getMessages());

        //处理审批信息
        Flux<Event> startFlux = buildMsgFromApproval(approvalResult, msgs);
        //携带前端传递的额外参数
        handlerForwardedProps(msgs, input.getForwardedProps());

        // Determine the event stream based on whether this is an approval resume
        Flux<Event> agentEvents;
        if (Objects.isNull(startFlux)) {
            agentEvents = agent.stream(msgs, options);
        } else {
            agentEvents = Flux.concat(startFlux, agent.stream(msgs, options));
        }

        return Flux.concat(
                        // Emit RUN_STARTED
                        Flux.just(new AguiEvent.RunStarted(threadId, runId)),
                        // Stream agent events and convert to AG-UI events
                        agentEvents.concatMapIterable(event -> convertEvent(event, state)),
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


    /**
     * 将额外传递的信息放到最后一条User消息中
     *
     * @param msgs
     * @param forwardedProps
     */
    private void handlerForwardedProps(List<Msg> msgs, Map<String, Object> forwardedProps) {
        if (CollectionUtils.isEmpty(msgs)) return;

        for (int i = msgs.size() - 1; i >= 0; i--) {
            Msg msg = msgs.get(i);
            if (MsgRole.USER == msg.getRole()) {
                msg.getMetadata().put("forwardedProps", forwardedProps);
                break;
            }
        }

    }


    /**
     * 从输入消息中提取 approval 消息。
     * approval 消息的 role 为 "approval"，content 为 JSON 格式的审批结果。
     */
    private AguiMessage extractApprovalMessage(RunAgentInput input) {
        List<AguiMessage> messages = input.getMessages();
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        AguiMessage last = messages.getLast();
        if ("approval".equalsIgnoreCase(last.getRole())) {
            return last;
        }
        return null;
    }

    /**
     * 解析 approval 消息的 content，构建 ApprovalResult。
     */
    private ApprovalResult parseApprovalResult(AguiMessage approvalMsg) {
        String content = approvalMsg.getContent();
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
        List<AguiMessage> messages = input.getMessages();
        List<AguiMessage> filtered = messages.stream()
                .filter(msg -> !"approval".equalsIgnoreCase(msg.getRole()))
                .toList();
        return RunAgentInput.builder()
                .threadId(input.getThreadId())
                .runId(input.getRunId())
                .messages(filtered)
                .tools(input.getTools())
                .context(input.getContext())
                .forwardedProps(input.getForwardedProps())
                .build();
    }


    private Flux<Event> buildMsgFromApproval(ApprovalResult approvalResult, List<Msg> msgs) {
        if (Objects.isNull(approvalResult)) {
            return null;
        }
        if (approvalResult.isApproved()) {
            return null;
        }

        // User rejected: build cancel message based on pause stage
        Msg lastResult = findLastAssistantMsg();

        if (MsgRole.ASSISTANT.equals(Optional.ofNullable(lastResult).map(Msg::getRole).orElse(null))
                && lastResult.getMetadata().containsKey(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)) {
            // POST_REASONING: tool not yet executed, build cancel ToolResultBlock
            List<ToolUseBlock> pendingTools = lastResult.getContentBlocks(ToolUseBlock.class);
            String cancelText = approvalResult.rejectReason() != null
                    ? "操作被用户拒绝，原因：" + approvalResult.rejectReason()
                    : "操作被用户拒绝";

            Msg msg = Msg.builder()
                    .role(MsgRole.TOOL)
                    .content(pendingTools.stream()
                            .map(t -> ToolResultBlock.of(t.getId(), t.getName(),
                                    TextBlock.builder().text(cancelText).build()))
                            .toArray(ToolResultBlock[]::new))
                    .build();

            msgs.add(msg);

            logger.debug("Resuming agent after rejection (POST_REASONING): {}", cancelText);
            return Flux.just(new Event(EventType.TOOL_RESULT, msg, true));
        } else if (MsgRole.TOOL.equals(Optional.ofNullable(lastResult).map(Msg::getRole).orElse(null))
                && lastResult.getMetadata().containsKey(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)) {
            // POST_ACTING: tool already executed, reject means stop further reasoning
            msgs.add(Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text("用户拒绝继续，终止本轮操作").build())
                    .build());

            logger.debug("Resuming agent after rejection (POST_ACTING)");

        }

        return null;
    }

    /**
     * 从 Agent 的 Memory 中获取最后一条消息。
     * 仅支持 ReActAgent 类型。
     */
    private Msg findLastAssistantMsg() {
        if (agent instanceof ReActAgent reactAgent) {
            List<Msg> messages = reactAgent.getMemory().getMessages();
            if (CollectionUtils.isEmpty(messages)) {
                return null;
            }
            return messages.getLast();
        }
        return null;
    }

    /**
     * Convert an AgentScope event to AG-UI events.
     *
     * @param event The AgentScope event
     * @param state The conversion state
     * @return List of AG-UI events
     */
    private List<AguiEvent> convertEvent(Event event, EventConversionState state) {
        List<AguiEvent> events = new ArrayList<>();
        Msg msg = event.getMessage();
        EventType type = event.getType();

        if (type == EventType.REASONING) {
            // Handle reasoning events - convert to text messages and tool calls
            for (ContentBlock block : msg.getContent()) {
                if (block instanceof TextBlock textBlock) {
                    String text = textBlock.getText();
                    if (text != null && !text.isEmpty()) {
                        String messageId = msg.getId();

                        // Start message if not started
                        if (!state.hasStartedMessage(messageId)) {
                            events.add(
                                    new AguiEvent.TextMessageStart(
                                            state.threadId, state.runId, messageId, "assistant"));
                            state.startMessage(messageId);
                        }

                        if (!event.isLast()) {
                            // In incremental mode, text is already the delta
                            events.add(
                                    new AguiEvent.TextMessageContent(
                                            state.threadId, state.runId, messageId, text));
                        } else {
                            // End message if this is the last event
                            if (!state.hasEndedMessage(messageId)) {
                                events.add(
                                        new AguiEvent.TextMessageEnd(
                                                state.threadId, state.runId, messageId));
                                state.endMessage(messageId);
                            }
                        }
                    }
                } else if (block instanceof ThinkingBlock thinkingBlock) {
                    // Handle thinking blocks - convert to REASONING_* events (only if enabled)
                    // According to AG-UI Reasoning draft: https://docs.ag-ui.com/drafts/reasoning
                    if (config.isEnableReasoning()) {
                        String thinking = thinkingBlock.getThinking();
                        if (thinking != null && !thinking.isEmpty()) {
                            String messageId = msg.getId();

                            // Start reasoning message if not started
                            if (!state.hasStartedReasoningMessage(messageId)) {
                                events.add(
                                        new AguiEvent.ReasoningMessageStart(
                                                state.threadId,
                                                state.runId,
                                                messageId,
                                                "assistant"));
                                state.startReasoningMessage(messageId);
                            }

                            if (!event.isLast()) {
                                // In incremental mode, thinking is already the delta
                                events.add(
                                        new AguiEvent.ReasoningMessageContent(
                                                state.threadId, state.runId, messageId, thinking));
                            } else {
                                // End reasoning message if this is the last event
                                events.add(
                                        new AguiEvent.ReasoningMessageEnd(
                                                state.threadId, state.runId, messageId));
                                state.endReasoningMessage(messageId);
                            }
                        }
                    }
                    // If reasoning is disabled, ThinkingBlock content is ignored (backward
                    // compatibility)
                } else if (block instanceof ToolUseBlock toolUse) {
                    // End any active text message before starting tool call
                    if (state.hasActiveTextMessage()) {
                        String activeMessageId = state.getCurrentTextMessageId();
                        events.add(
                                new AguiEvent.TextMessageEnd(
                                        state.threadId, state.runId, activeMessageId));
                        state.endMessage(activeMessageId);
                    }

                    // Emit tool call start
                    String toolCallId = toolUse.getId();
                    if (toolCallId == null) {
                        toolCallId = UUID.randomUUID().toString();
                    }

                    if (!state.hasStartedToolCall(toolCallId)) {
                        events.add(
                                new AguiEvent.ToolCallStart(
                                        state.threadId,
                                        state.runId,
                                        toolCallId,
                                        toolUse.getName()));
                        state.startToolCall(toolCallId);
                    }

                    // Emit tool call args if enabled
                    if (config.isEmitToolCallArgs() && !event.isLast()) {
                        String args = toolUse.getContent();
                        if (args != null && !args.isEmpty()) {
                            events.add(
                                    new AguiEvent.ToolCallArgs(
                                            state.threadId, state.runId, toolCallId, args));
                        }
                    }
                }
            }
        } else if (type == EventType.TOOL_RESULT && event.isLast()) {
            // Handle tool results
            for (ContentBlock block : msg.getContent()) {
                if (block instanceof ToolResultBlock toolResult) {
                    String toolCallId = toolResult.getId();
                    String result = extractToolResultText(toolResult);

                    boolean hasStarted = state.hasStartedToolCall(toolCallId);
                    if (hasStarted) {
//                        events.add(
//                                new AguiEvent.ToolCallStart(
//                                        state.threadId, state.runId, toolCallId, "unknown"));
//                        state.startToolCall(toolCallId);
                        // Ensure ToolCallEnd is emitted to close arguments phase
                        events.add(new AguiEvent.ToolCallEnd(state.threadId, state.runId, toolCallId));
                    }


                    events.add(
                            new AguiEvent.ToolCallResult(
                                    state.threadId,
                                    state.runId,
                                    toolCallId,
                                    result,
                                    "tool",
                                    msg.getId()));
                    state.endToolCall(toolCallId);
                }
            }
        } else if (type == EventType.AGENT_RESULT) {
            //人工中断发送自定义事件
            GenerateReason generateReason = msg.getGenerateReason();

            if (GenerateReason.REASONING_STOP_REQUESTED == generateReason) {
                List<ToolUseBlock> contentBlocks = msg.getContentBlocks(ToolUseBlock.class);
                List<ApprovalTips> approvalToolNames = Optional.ofNullable((List<ApprovalTips>) msg.getMetadata().get(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)).orElse(Collections.emptyList());

                List<HumanApprovalInfo.ReasoningStateInfo> approvalTools = approvalToolNames.stream()
                        .map(t -> {

                            Optional<ToolUseBlock> toolUseBlock = contentBlocks.stream().filter(c -> c.getName().equals(t.toolName()))
                                    .findFirst();

                            return new HumanApprovalInfo.ReasoningStateInfo(t.tip(), toolUseBlock.orElse(null));

                        })
                        .toList();

                if (CollUtil.isNotEmpty(approvalTools)) {
                    events.add(
                            new AguiEvent.Custom(state.threadId, state.runId,
                                    AIConstants.AguiCustomEvent.HUMAN_APPROVAL,
                                    new HumanApprovalInfo(ApprovalStage.POST_REASONING, approvalTools, null))
                    );
                }
            } else if (GenerateReason.ACTING_STOP_REQUESTED == generateReason) {
                List<ToolResultBlock> contentBlocks = msg.getContentBlocks(ToolResultBlock.class);
                List<ApprovalTips> approvalToolNames = Optional.ofNullable((List<ApprovalTips>) msg.getMetadata().get(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)).orElse(Collections.emptyList());
                approvalToolNames.stream()
                        .map(t -> {
                            Optional<ToolResultBlock> resultBlock = contentBlocks.stream().filter(c -> c.getName().equals(t.toolName()))
                                    .findFirst();
                            return new HumanApprovalInfo.ActingStageInfo(t.tip(), resultBlock.orElse(null));
                        }).findFirst().ifPresent(stageInfo ->

                                events.add(
                                        new AguiEvent.Custom(state.threadId, state.runId,
                                                AIConstants.AguiCustomEvent.HUMAN_APPROVAL,
                                                new HumanApprovalInfo(ApprovalStage.POST_ACTING, null, stageInfo)
                                        )
                                )

                        );

            }
        }

        return events;
    }

    /**
     * Finish the run by emitting any pending end events and RUN_FINISHED.
     *
     * @param state The conversion state
     * @return Flux of final events
     */
    private Flux<AguiEvent> finishRun(EventConversionState state) {
        List<AguiEvent> events = new ArrayList<>();

        // End any messages that weren't properly ended
        for (String messageId : state.getStartedMessages()) {
            if (!state.hasEndedMessage(messageId)) {
                events.add(new AguiEvent.TextMessageEnd(state.threadId, state.runId, messageId));
            }
        }

        // End any tool calls that weren't properly ended
        for (String toolCallId : state.getStartedToolCalls()) {
            if (!state.hasEndedToolCall(toolCallId)) {
                events.add(new AguiEvent.ToolCallEnd(state.threadId, state.runId, toolCallId));
            }
        }

        // End any reasoning messages that weren't properly ended
        for (String messageId : state.getStartedReasoningMessages()) {
            if (!state.hasEndedReasoningMessage(messageId)) {
                events.add(
                        new AguiEvent.ReasoningMessageEnd(state.threadId, state.runId, messageId));
            }
        }

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
        private final Set<String> startedMessages = new LinkedHashSet<>();
        private final Set<String> endedMessages = new LinkedHashSet<>();
        private final Set<String> startedToolCalls = new LinkedHashSet<>();
        private final Set<String> endedToolCalls = new LinkedHashSet<>();
        private final Set<String> startedReasoningMessages = new LinkedHashSet<>();
        private final Set<String> endedReasoningMessages = new LinkedHashSet<>();
        private String currentTextMessageId = null;

        EventConversionState(String threadId, String runId) {
            this.threadId = threadId;
            this.runId = runId;
        }

        boolean hasStartedMessage(String messageId) {
            return startedMessages.contains(messageId);
        }

        void startMessage(String messageId) {
            startedMessages.add(messageId);
            currentTextMessageId = messageId;
        }

        void endMessage(String messageId) {
            endedMessages.add(messageId);
            if (messageId.equals(currentTextMessageId)) {
                currentTextMessageId = null;
            }
        }

        boolean hasEndedMessage(String messageId) {
            return endedMessages.contains(messageId);
        }

        String getCurrentTextMessageId() {
            return currentTextMessageId;
        }

        boolean hasActiveTextMessage() {
            return currentTextMessageId != null && !hasEndedMessage(currentTextMessageId);
        }

        Set<String> getStartedMessages() {
            return startedMessages;
        }

        boolean hasStartedToolCall(String toolCallId) {
            return startedToolCalls.contains(toolCallId);
        }

        void startToolCall(String toolCallId) {
            startedToolCalls.add(toolCallId);
        }

        void endToolCall(String toolCallId) {
            endedToolCalls.add(toolCallId);
        }

        boolean hasEndedToolCall(String toolCallId) {
            return endedToolCalls.contains(toolCallId);
        }

        Set<String> getStartedToolCalls() {
            return startedToolCalls;
        }

        boolean hasStartedReasoningMessage(String messageId) {
            return startedReasoningMessages.contains(messageId);
        }

        void startReasoningMessage(String messageId) {
            startedReasoningMessages.add(messageId);
        }

        void endReasoningMessage(String messageId) {
            endedReasoningMessages.add(messageId);
        }

        boolean hasEndedReasoningMessage(String messageId) {
            return endedReasoningMessages.contains(messageId);
        }

        Set<String> getStartedReasoningMessages() {
            return startedReasoningMessages;
        }
    }
}
