package org.quyq.gwsu.headless.service.impl;


import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.dto.HeadlessResourceDTO;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.core.session.HeadlessAccessSession;
import org.quyq.gwsu.headless.domain.HeadlessCallConfig;
import org.quyq.gwsu.headless.domain.RouterInfo;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import org.quyq.gwsu.headless.enums.GraphRouteType;
import org.quyq.gwsu.headless.errcode.HeadlessErrorCode;
import org.quyq.gwsu.headless.graph.IntentRecognitionNode;
import org.quyq.gwsu.headless.graph.HeadlessAguiEventBridge;
import org.quyq.gwsu.headless.graph.SendAnswerNode;
import org.quyq.gwsu.headless.graph.SendApprovalNode;
import org.quyq.gwsu.headless.graph.SendChatNode;
import org.quyq.gwsu.headless.service.IHeadlessService;
import org.redisson.api.RLock;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeadlessServiceImpl implements IHeadlessService, InitializingBean {

    private final AgentStateStore agentStateStore;

    private final HeadlessBrowserManager headlessBrowserManager;

    private final SecurityUtils securityUtils;

    private final CacheUtils cacheUtils;

    public CompiledGraph headlessGraph;

    private final String lockKey = "headless:lock:%s";


    @SneakyThrows
    @Override
    public Flux<AguiEvent> stream(HeadlessDTO request, HeadlessCallConfig config) {
        AssertUtils.notNull(request, HeadlessErrorCode.E01006);
        validateRequest(request);
        String query = buildRoutingQuery(request);
        String userId = config.getUserId();
        if (!StringUtils.hasText(userId)) {
            userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse("");
        }
        SubjectInfo subjectInfo = new SubjectInfo(config.getSign(), userId);
        AssertUtils.hasText(userId, HeadlessErrorCode.E01005);

        String threadId = StringUtils.hasText(config.getThreadId()) ? config.getThreadId()
                : Optional.ofNullable(headlessBrowserManager.getAccessSession(subjectInfo))
                .map(HeadlessAccessSession::threadId)
                .orElse(IdUtil.fastUUID());
        AtomicReference<String> runIdRef = new AtomicReference<>("");

        String keySign = StringUtils.hasText(config.getSign()) ? config.getSign() + ":" + userId : userId;
        RLock lock = cacheUtils.getLock(lockKey.formatted(keySign));
        // leaseTime=-1：锁不自动过期，持有到操作完成才释放
        boolean acquired = lock.tryLock(0, -1, TimeUnit.SECONDS);
        if (!acquired) {
            return Flux.just(
                    HeadlessAguiEventBridge.statusEvent(threadId, "", org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus.BUSY),
                    HeadlessAguiEventBridge.rawEvent(threadId, "", "BUSY", "助手正在回答中，请稍后尝试...")
            );
        }

        return headlessGraph.stream(Map.of(
                        HeadlessConstants.Headless.GRAPH_PARAM_QUERY, query,
                        HeadlessConstants.Headless.GRAPH_PARAM_REQUEST, request,
                        HeadlessConstants.Headless.GRAPH_PARAM_USER_ID,subjectInfo ,
                        HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, threadId
                ))
                .filter(nodeOutput -> nodeOutput instanceof StreamingOutput<?>)
                .map(nodeOutput -> (StreamingOutput<?>) nodeOutput)
                .filter(output -> Objects.nonNull(output.message())
                        && output.getOutputType() != null
                        && (output.getOutputType() == OutputType.AGENT_MODEL_STREAMING
                        || output.getOutputType() == OutputType.AGENT_TOOL_STREAMING
                        || output.getOutputType() == OutputType.AGENT_HOOK_STREAMING
                        || output.getOutputType() == OutputType.GRAPH_NODE_STREAMING
                ))
                .map(StreamingOutput::message)
                .cast(AssistantMessage.class)
                .map(message -> HeadlessAguiEventBridge.fromChatResponse(new ChatResponse(
                        List.of(new Generation(message))
                )))
                .filter(Objects::nonNull)
                .doOnNext(event -> {
                    if (StringUtils.hasText(event.getRunId())) {
                        runIdRef.set(event.getRunId());
                    }
                })
                .startWith(
                        Flux.just(HeadlessAguiEventBridge.statusEvent(threadId, "", HeadlessAgentStatus.CONNECTION))
                )
                // 将异常转为 ERROR 事件，让 SSE 流优雅结束，而非暴力断开连接
                .onErrorResume(error -> {
                    log.error("智能体流式处理异常: {}", error.getMessage(), error);
                    return Flux.just(
                            HeadlessAguiEventBridge.statusEvent(threadId, runIdRef.get(),
                                    HeadlessAgentStatus.ERROR),
                            HeadlessAguiEventBridge.rawEvent(threadId, runIdRef.get(), error.getMessage())
                    );
                })

                .doFinally(signal -> {
                    try {
                        if (lock.isLocked()) {
                            lock.forceUnlock();
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                });
    }


    public CompiledGraph buildGraph() throws GraphStateException {

        IntentRecognitionNode intentRecognitionNode = new IntentRecognitionNode(agentStateStore, headlessBrowserManager);
        SendChatNode sendChatNode = new SendChatNode(agentStateStore, headlessBrowserManager);
        SendAnswerNode sendAnswerNode = new SendAnswerNode(agentStateStore, headlessBrowserManager);
        SendApprovalNode sendApprovalNode = new SendApprovalNode(agentStateStore, headlessBrowserManager);

        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();

            keyStrategyMap.put(HeadlessConstants.Headless.GRAPH_PARAM_QUERY, new ReplaceStrategy());
            keyStrategyMap.put(HeadlessConstants.Headless.GRAPH_PARAM_REQUEST, new ReplaceStrategy());
            keyStrategyMap.put(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, new ReplaceStrategy());
            keyStrategyMap.put(HeadlessConstants.Headless.GRAPH_PARAM_USER_ID, new ReplaceStrategy());
            keyStrategyMap.put(HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO, new ReplaceStrategy());
            keyStrategyMap.put(HeadlessConstants.Headless.GRAPH_PARAM_OUTPUT, new ReplaceStrategy());

            return keyStrategyMap;

        };

        StateGraph graph = new StateGraph(keyStrategyFactory)
                .addNode("intentRecognitionNode", AsyncNodeAction.node_async(intentRecognitionNode))
                .addNode("sendChatNode", AsyncNodeAction.node_async(sendChatNode))
                .addNode("sendAnswerNode", AsyncNodeAction.node_async(sendAnswerNode))
                .addNode("sendApprovalNode", AsyncNodeAction.node_async(sendApprovalNode))
                .addEdge(StateGraph.START, "intentRecognitionNode")
                .addConditionalEdges("intentRecognitionNode", AsyncEdgeAction.edge_async(state ->
                                state.value(HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO, RouterInfo.class)
                                        .map(v -> v.getType().name()).orElse(GraphRouteType.UNKNOWN.name())
                        ),
                        Map.of(
                                "UNKNOWN", StateGraph.END,
                                "CHAT", "sendChatNode",
                                "APPROVAL", "sendApprovalNode",
                                "ANSWER", "sendAnswerNode"
                        )
                )
                .addEdge("sendChatNode", StateGraph.END)
                .addEdge("sendAnswerNode", StateGraph.END)
                .addEdge("sendApprovalNode", StateGraph.END);


        return graph.compile(CompileConfig.builder()
                .build());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        headlessGraph = buildGraph();
    }

    private void validateRequest(HeadlessDTO request) {
        if (request.resources() != null) {
            for (HeadlessResourceDTO resource : request.resources()) {
                AssertUtils.notNull(resource, HeadlessErrorCode.E01006);
                AssertUtils.hasText(resource.url(), HeadlessErrorCode.E01006);
                AssertUtils.hasText(resource.mimeType(), HeadlessErrorCode.E01006);
            }
        }
        AssertUtils.isTrue(request.hasContent(), HeadlessErrorCode.E01006);
    }

    private String buildRoutingQuery(HeadlessDTO request) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(request.text())) {
            parts.add(request.text().trim());
        }
        if (!CollectionUtils.isEmpty(request.resources())) {
            String resourceSummary = request.resources().stream()
                    .filter(Objects::nonNull)
                    .map(this::describeResource)
                    .filter(StringUtils::hasText)
                    .reduce((left, right) -> left + "；" + right)
                    .orElse("用户附带了资源");
            parts.add("用户附带了%s个资源：%s".formatted(request.resources().size(), resourceSummary));
        }
        return String.join("\n", parts);
    }

    private String describeResource(HeadlessResourceDTO resource) {
        if (resource == null || !StringUtils.hasText(resource.url())) {
            return null;
        }
        String mimeType = StringUtils.hasText(resource.mimeType()) ? resource.mimeType() : "unknown";
        return "资源(mimeType=%s)".formatted(mimeType);
    }
}
