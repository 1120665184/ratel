package org.quyq.gwsu.common.ai.agui;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.core.agui.AguiException;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.session.Session;
import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.domain.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.domain.CopilotKitInfo;
import org.quyq.gwsu.common.ai.agui.dto.ChatDTO;
import org.quyq.gwsu.common.ai.agui.push.AguiEventPusher;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.quyq.gwsu.common.ai.session.CommonSessionKey;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.accessor.HeadersContextThreadLocalAccessor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.util.context.Context;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/4/23
 * @description
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AguiController implements DisposableBean {

    private final static String AGENT_STOP_EVENT_TOPIC = "AGENT_STOP_EVENT_TOPIC:";

    private final AguiRequestProcessor processor;

    private final WebToolUtils webToolUtils;

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final long sseTimeout;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    private final static EmitterWrapperManager CURR_EMITTER = new EmitterWrapperManager();

    private final Gson gson = new Gson();

    private RedisMessageListenerContainer listenerContainer = null;

    private final List<AguiEventPusher> pushers = new ArrayList<>();


    @Setter
    private Session agentSession;


    public static AIRunnerInstanceWrapper getCurrEmitter(String threadId) {
        return CURR_EMITTER.get(threadId);
    }

    public void addPusher(AguiEventPusher pusher) {
        pushers.add(pusher);
    }


    @Override
    public void destroy() throws Exception {
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
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
                    .body(handleAgentStop(request, headerAgentId));
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
    private String getCurrUserId() {
        return securityUtils.userInfo().map(UserInfo::getUserId).orElse(null);
    }


    protected SseEmitter handlerAgentConnect(ChatDTO request) {
        SseEmitter emitter = new SseEmitter(sseTimeout);
        RunAgentInput body = request.body();
        AIRunnerInstanceWrapper wrapper = new AIRunnerInstanceWrapper(request.body(), emitter, false, pushers);
        executorService.submit(() -> {
            wrapper.sendEvent(new AguiEvent.RunStarted(body.getThreadId(), body.getRunId()));
            wrapper.sendEvent(new AguiEvent.RunFinished(body.getThreadId(), body.getRunId()));
            emitter.complete();
        });

        return emitter;
    }

    /**
     * 处理 agent/run 和 agent/connect 请求
     * 返回 SSE 流
     */
    protected SseEmitter handleAgentRun(ChatDTO request, String headerAgentId) {
        if (!DeployUtils.isSingle()) {
            initStopListenerContainer();
        }

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
    protected Map<String, Object> handleAgentStop(ChatDTO request, String headerAgentId) {
        Map<String, Object> p = request.params();
        // 从 params 中获取 agentId 和 threadId
        String agentId = Optional.ofNullable(p.get("agentId")).map(String::valueOf).orElse(headerAgentId);
        String threadId = Optional.ofNullable(p.get("threadId")).map(String::valueOf).orElse(null);

        log.info("Agent stop requested: agentId={}, threadId={}", agentId, threadId);
        if (StringUtils.hasText(threadId) && Objects.isNull(getCurrEmitter(threadId))) {
            SpringUtils.getBean(CacheUtils.class)
                    .convertAndSend(getStopEventTopic(), new StopEventInfo(agentId, threadId));
        } else {
            stopAgent(agentId, threadId);
        }


        return Map.of("success", true);
    }

    private void stopAgent(String agentId, String threadId) {
        if (StringUtils.hasText(threadId)) {
            AIRunnerInstanceWrapper currEmitter = getCurrEmitter(threadId);
            if (Objects.nonNull(currEmitter)) {
                processor.interrupt(agentId, threadId);
                currEmitter.emitter().complete();
            }

        }
    }

    /**
     * 处理 AG-UI 请求的核心逻辑
     */
    private SseEmitter handleInternal(
            RunAgentInput input, String headerAgentId, String pathAgentId) {
        SseEmitter emitter = new SseEmitter(sseTimeout);
        String threadId = input.getThreadId();
        String runId = input.getRunId();
        String userId = getCurrUserId();

        // 在Servlet线程上提前捕获headers，避免进入虚拟线程后
        // processor.process()内部的enableAutomaticContextPropagation清除ThreadLocal
        Map<String, String> capturedHeaders = ServletUtils.LOCAL_HEADERS.get();
        //传递到reactor中，保证tid正确
        Observation observation = ObservationThreadLocalAccessor.getInstance().getValue();

        AIRunnerInstanceWrapper wrapper = new AIRunnerInstanceWrapper(input, emitter, isHeadless(), pushers);
        executorService.submit(
                () -> {
                    Disposable subscription;
                    try {

                        // Process request - returns both agent and event stream
                        AguiRequestProcessor.ProcessResult result =
                                processor.process(input, headerAgentId, pathAgentId, userId);
                        CURR_EMITTER.put(threadId, wrapper);
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
                                        .contextCapture()
                                        .contextWrite(Context.of(
                                                AIConstants.Param.THREAD_ID, threadId
                                                , AIConstants.Param.EMITTER_WRAPPER, wrapper
                                                , HeadersContextThreadLocalAccessor.REACTOR_CONTEXT, capturedHeaders
                                                , ObservationThreadLocalAccessor.KEY, observation
                                        ))
                                        .subscribe(
                                                event -> sendEvent(wrapper, event),
                                                error -> {
                                                    log.error(
                                                            "Error during AG-UI run: {}",
                                                            error.getMessage());
                                                    sendErrorAndComplete(
                                                            wrapper,
                                                            error.getMessage());
                                                },
                                                () -> {
                                                    try {
                                                        emitter.complete();
                                                    } catch (Exception e) {
                                                        log.debug(
                                                                "Error completing emitter: {}",
                                                                e.getMessage());
                                                    }
                                                });

                    } catch (AguiException.AgentNotFoundException e) {
                        log.error("Agent not found: {}", e.getMessage());
                        sendErrorAndComplete(wrapper, e.getMessage());
                    } catch (Exception e) {
                        log.error("Error processing AG-UI request: {}", e.getMessage());
                        sendErrorAndComplete(wrapper, e.getMessage());
                    }
                });

        return emitter;
    }

    /**
     * 是否是无头浏览形式访问
     *
     * @return
     */
    private boolean isHeadless() {
        String loginType = sessionUtils.getLoginType();
        return "headless".equals(loginType);
    }

    private void sendEvent(AIRunnerInstanceWrapper wrapper, AguiEvent event) {
        wrapper.sendEvent(event);
    }

    private void sendErrorAndComplete(
            AIRunnerInstanceWrapper wrapper, String errorMessage) {
        RunAgentInput param = wrapper.input();
        AguiEvent.Raw errorEvent = new AguiEvent.Raw(param.getThreadId(), param.getRunId(), Map.of("error", errorMessage));
        AguiEvent.RunFinished finishEvent = new AguiEvent.RunFinished(param.getThreadId(), param.getRunId());

        wrapper.sendEvent(errorEvent);
        wrapper.sendEvent(finishEvent);

    }


    /**
     * 初始化智能体停止事件监听器
     * 为了解决分布式部署时，停止事件发送到其他微服务实例导致停止失败的问题
     */
    private void initStopListenerContainer() {
        if (Objects.nonNull(listenerContainer)) {
            return;
        }
        synchronized (AguiController.class) {
            if (Objects.isNull(listenerContainer)) {
                CacheUtils cacheUtils = SpringUtils.getBean(CacheUtils.class);
                listenerContainer = cacheUtils.addListener(getStopEventTopic(),
                        (message, pattern) -> {
                            Object msg = cacheUtils.getSerializer().deserialize(message.getBody());
                            if (msg instanceof StopEventInfo event) {
                                stopAgent(event.agentId, event.threadId);
                            }

                        });
            }
        }
    }

    private String getStopEventTopic() {
        return AGENT_STOP_EVENT_TOPIC + this.getClass().getName();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StopEventInfo {
        private String agentId;
        private String threadId;

    }


}
