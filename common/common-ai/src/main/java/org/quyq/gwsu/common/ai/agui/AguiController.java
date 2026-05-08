package org.quyq.gwsu.common.ai.agui;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agui.AguiException;
import io.agentscope.core.agui.encoder.AguiEventEncoder;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.domain.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.domain.CopilotKitInfo;
import org.quyq.gwsu.common.ai.agui.dto.ChatDTO;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.quyq.gwsu.common.ai.session.CommonSessionKey;
import org.quyq.gwsu.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Quyq
 * @date 2026/4/23
 * @description
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AguiController {

    private final AguiRequestProcessor processor;

    private final WebToolUtils webToolUtils;

    private final long sseTimeout;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private final AguiEventEncoder encoder = new AguiEventEncoder();

    private final static Map<String, AIRunnerInstanceWrapper> CURR_EMITTER = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();


    @Setter
    private Session agentSession;


    public static AIRunnerInstanceWrapper getCurrEmitter(String threadId) {
        return CURR_EMITTER.get(threadId);
    }

    /**
     * CopilotKit Single Endpoint 统一入口
     * <p>
     * 根据 method 字段路由到不同的处理逻辑：
     * - info: 返回 runtime 信息（JSON）
     * - agent/connect: 返回 SSE 流
     * - agent/run: 返回 SSE 流
     * - agent/stop: 停止 agent（JSON）
     */
    public ResponseEntity<?> handleCopilotKitRequest(ChatDTO request, String headerAgentId) {

        return switch (request.method()) {
            case "info" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(handleInfo());
            case "agent/connect" -> ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(handlerAgentConnect(request));
            case "agent/run" -> ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(handleAgentRun(request, headerAgentId));
            case "agent/stop" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(handleAgentStop(request));
            default -> ResponseEntity.badRequest().body(R.fail("未知的方法：%s".formatted(request.method())));
        };

    }

    /**
     * 处理前端工具执行结果回调
     *
     * @param request 回调请求
     * @return 处理结果
     */
    public R<Void> handleToolCallback(WebToolCallbackRequest request) {
        boolean success = webToolUtils.handleCallback(request.toolCallId(), request.success(), request.result());
        if (!success) {
            return R.fail("工具回调处理失败: " + request.toolCallId());
        }
        return R.ok();
    }

    /**
     * 处理审批状态查询
     * 从 Session 中加载 Agent，检查是否处于 STOP_REQUESTED 状态
     *
     * @param threadId 会话ID
     * @return 审批状态信息
     */
    public R<HumanApprovalInfo> handleApprovalStatus(String threadId) {
        if (agentSession == null) {
            return R.ok(new HumanApprovalInfo(null, null, null));
        }

        try {
            String userId = getCurrUserId();
            CommonSessionKey sessionKey = CommonSessionKey.of(threadId, userId);

            // 从 Session 中直接加载 Memory 消息，判断暂停阶段
            List<Msg> messages = agentSession.getList(sessionKey, "memory_messages", Msg.class);
            Msg lastResult = findLastAssistantMsg(messages);
            if (lastResult == null) {
                return R.ok(new HumanApprovalInfo(null, null, null));
            }


            if (MsgRole.ASSISTANT.equals(lastResult.getRole())
                    && lastResult.getMetadata().containsKey(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)) {
                List<ToolUseBlock> contentBlocks = lastResult.getContentBlocks(ToolUseBlock.class);

                List<ApprovalTips> approvalToolNames = Optional.ofNullable(
                                lastResult.getMetadata().get(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)
                        ).map(v -> gson.fromJson(gson.toJson(v), new TypeToken<List<ApprovalTips>>() {
                        }))
                        .orElse(Collections.emptyList());

                List<HumanApprovalInfo.ReasoningStateInfo> reasoningInfo = approvalToolNames.stream()
                        .map(t -> {
                            Optional<ToolUseBlock> toolUseBlock = contentBlocks.stream()
                                    .filter(c -> c.getName().equals(t.toolName()))
                                    .findFirst();
                            return new HumanApprovalInfo.ReasoningStateInfo(t.tip(), toolUseBlock.orElse(null));
                        })
                        .toList();

                return R.ok(new HumanApprovalInfo(ApprovalStage.POST_REASONING, reasoningInfo, null));
            } else if (MsgRole.TOOL.equals(lastResult.getRole())
                    && lastResult.getMetadata().containsKey(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)) {
                List<ToolResultBlock> contentBlocks = lastResult.getContentBlocks(ToolResultBlock.class);

                List<ApprovalTips> approvalToolNames = Optional.ofNullable(
                                lastResult.getMetadata().get(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)
                        ).map(v -> gson.fromJson(gson.toJson(v), new TypeToken<List<ApprovalTips>>() {
                        }))
                        .orElse(Collections.emptyList());

                return approvalToolNames.stream()
                        .map(t -> {
                            Optional<ToolResultBlock> resultBlock = contentBlocks.stream()
                                    .filter(c -> c.getName().equals(t.toolName()))
                                    .findFirst();
                            return new HumanApprovalInfo.ActingStageInfo(t.tip(), resultBlock.orElse(null));
                        })
                        .findFirst()
                        .map(stageInfo -> R.ok(new HumanApprovalInfo(ApprovalStage.POST_ACTING, null, stageInfo)))
                        .orElse(R.ok(new HumanApprovalInfo(null, null, null)));
            }

            return R.ok(new HumanApprovalInfo(null, null, null));
        } catch (Exception e) {
            log.error("Failed to query approval status for thread {}: {}", threadId, e.getMessage());
            return R.ok(new HumanApprovalInfo(null, null, null));
        }
    }

    /**
     * 从消息列表中获取最后一条消息
     */
    private Msg findLastAssistantMsg(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.getLast();
    }

    /**
     * agui格式统一入口
     *
     * @param input
     * @param agentId
     * @return
     */
    public SseEmitter handleAgui(
            RunAgentInput input, String agentId) {
        return handleInternal(input, agentId, null);
    }


    /**
     * 处理 info 请求
     * 返回 runtime 信息和可用的 agents
     */
    protected abstract CopilotKitInfo handleInfo();


    /**
     * 获取当前登录的用户ID
     *
     * @return
     */
    protected abstract String getCurrUserId();


    protected SseEmitter handlerAgentConnect(ChatDTO request) {
        SseEmitter emitter = new SseEmitter(sseTimeout);
        RunAgentInput body = request.body();
        executorService.submit(() -> {
            sendEvent(emitter, new AguiEvent.RunStarted(body.getThreadId(), body.getRunId()));
            sendEvent(emitter, new AguiEvent.RunFinished(body.getThreadId(), body.getRunId()));
            emitter.complete();
        });

        return emitter;
    }

    /**
     * 处理 agent/run 和 agent/connect 请求
     * 返回 SSE 流
     */
    protected SseEmitter handleAgentRun(ChatDTO request, String headerAgentId) {
        RunAgentInput input = request.body();

        // 从 params 中获取 agentId（如果有）
        String pathAgentId = null;
        if (!CollectionUtils.isEmpty(request.params()) && request.params().containsKey("agentId")) {
            pathAgentId = (String) request.params().get("agentId");
        }

        return handleInternal(input, headerAgentId, pathAgentId);
    }

    /**
     * 处理 agent/stop 请求
     */
    protected Map<String, Object> handleAgentStop(ChatDTO request) {
        Map<String, Object> p = request.params();
        // 从 params 中获取 agentId 和 threadId
        String agentId = Optional.ofNullable(p.get("agentId")).map(String::valueOf).orElse(null);
        Optional<String> threadId = Optional.ofNullable(p.get("threadId")).map(String::valueOf);

        log.info("Agent stop requested: agentId={}, threadId={}", agentId, threadId.orElse(null));


        return Map.of("success", true);
    }

    /**
     * 处理 AG-UI 请求的核心逻辑
     */
    private SseEmitter handleInternal(
            RunAgentInput input, String headerAgentId, String pathAgentId) {
        SseEmitter emitter = new SseEmitter(sseTimeout);
        String threadId = input.getThreadId();
        String runId = input.getRunId();
        executorService.submit(
                () -> {
                    Disposable subscription;
                    try {
                        // Process request - returns both agent and event stream
                        AguiRequestProcessor.ProcessResult result =
                                processor.process(input, headerAgentId, pathAgentId);
                        CURR_EMITTER.put(threadId, new AIRunnerInstanceWrapper(input, emitter));
                        // Set up callbacks for client disconnect handling
                        emitter.onCompletion(
                                () -> {
                                    log.debug("SSE connection completed for run {}", runId);
                                    CURR_EMITTER.remove(threadId);
                                });
                        emitter.onTimeout(
                                () -> {
                                    log.info(
                                            "SSE connection timed out for run {}, interrupting agent",
                                            runId);
                                    CURR_EMITTER.remove(threadId);
                                    result.agent().interrupt();
                                });
                        emitter.onError(
                                (ex) -> {
                                    log.info(
                                            "SSE connection error for run {}: {}, interrupting agent",
                                            runId,
                                            ex.getMessage());
                                    CURR_EMITTER.remove(threadId);
                                    result.agent().interrupt();
                                });

                        // Subscribe to event stream
                        subscription =
                                result.events()
                                        .subscribe(
                                                event -> sendEvent(emitter, event),
                                                error -> {
                                                    log.error(
                                                            "Error during AG-UI run: {}",
                                                            error.getMessage());
                                                    sendErrorAndComplete(
                                                            emitter,
                                                            threadId,
                                                            runId,
                                                            error.getMessage());
                                                },
                                                () -> {
                                                    try {
                                                        emitter.complete();
                                                        //持久化
                                                        if (Objects.nonNull(agentSession) && result.agent() instanceof ReActAgent raa) {
                                                            raa.saveTo(agentSession, CommonSessionKey.of(threadId, getCurrUserId()));
                                                        }
                                                    } catch (Exception e) {
                                                        log.debug(
                                                                "Error completing emitter: {}",
                                                                e.getMessage());
                                                    }
                                                });

                    } catch (AguiException.AgentNotFoundException e) {
                        log.error("Agent not found: {}", e.getMessage());
                        sendErrorAndComplete(emitter, threadId, runId, e.getMessage());
                    } catch (Exception e) {
                        log.error("Error processing AG-UI request: {}", e.getMessage());
                        sendErrorAndComplete(emitter, threadId, runId, e.getMessage());
                    }
                });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, AguiEvent event) {
        try {
            String jsonData = encoder.encodeToJson(event);
            emitter.send(SseEmitter.event().data(jsonData, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.debug("Failed to send SSE event: {}", e.getMessage());
        }
    }

    private void sendErrorAndComplete(
            SseEmitter emitter, String threadId, String runId, String errorMessage) {
        try {
            String errorJson =
                    encoder.encodeToJson(
                            new AguiEvent.Raw(threadId, runId, Map.of("error", errorMessage)));
            String finishJson = encoder.encodeToJson(new AguiEvent.RunFinished(threadId, runId));
            emitter.send(SseEmitter.event().data(errorJson, MediaType.APPLICATION_JSON));
            emitter.send(SseEmitter.event().data(finishJson, MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (IOException e) {
            log.debug("Failed to send error event: {}", e.getMessage());
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.debug("Failed to complete emitter with error: {}", ex.getMessage());
            }
        }
    }


}
