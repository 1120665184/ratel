package org.quyq.gwsu.headless.service.impl;


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
import org.quyq.gwsu.common.ai.utils.AgentScopeMessageUtils;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.headless.api.dto.ContentBlock;
import org.quyq.gwsu.headless.api.dto.UserMsg;
import org.quyq.gwsu.headless.api.dto.block.*;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.api.vo.AssistantMsg;
import org.quyq.gwsu.headless.api.vo.HeadlessResponse;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.domain.HeadlessCallConfig;
import org.quyq.gwsu.headless.domain.RouterInfo;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import org.quyq.gwsu.headless.enums.GraphRouteType;
import org.quyq.gwsu.headless.errcode.HeadlessErrorCode;
import org.quyq.gwsu.headless.graph.IntentRecognitionNode;
import org.quyq.gwsu.headless.graph.SendAnswerNode;
import org.quyq.gwsu.headless.graph.SendApprovalNode;
import org.quyq.gwsu.headless.graph.SendChatNode;
import org.quyq.gwsu.headless.service.IHeadlessService;
import org.redisson.api.RLock;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.TimeUnit;

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
    public Flux<HeadlessResponse> stream(UserMsg msg, HeadlessCallConfig config) {
        String query = msg.getTextContent();
        AssertUtils.hasText(query, HeadlessErrorCode.E01006);
        String userId = config.getUserId();
        if (!StringUtils.hasText(userId)) {
            userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse("");
        }
        AssertUtils.hasText(userId, HeadlessErrorCode.E01005);

        String keySign = StringUtils.hasText(config.getSign()) ? config.getSign() + ":" + userId : userId;
        RLock lock = cacheUtils.getLock(lockKey.formatted(keySign));
        // leaseTime=-1：锁不自动过期，持有到操作完成才释放
        boolean acquired = lock.tryLock(0, -1, TimeUnit.SECONDS);
        if (!acquired) {
            return Flux.just(HeadlessResponse.busy());
        }

        return headlessGraph.stream(Map.of(
                        HeadlessConstants.Headless.GRAPH_PARAM_QUERY, query,
                        HeadlessConstants.Headless.GRAPH_PARAM_USER_ID, new SubjectInfo(config.getSign(), userId),
                        HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, Optional.ofNullable(config.getThreadId()).orElse("")
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
                .flatMap(streamingOutput -> {
                    AssistantMessage aiMessage = (AssistantMessage) streamingOutput.message();
                    HeadlessAgentStatus status = (HeadlessAgentStatus) aiMessage.getMetadata().get("status");

                    AssistantMsg assistantMsg = convertToAssistantMsg(aiMessage);
                    return Flux.just(new HeadlessResponse(status, assistantMsg));
                })
                .startWith(
                        Flux.just(new HeadlessResponse(HeadlessAgentStatus.CONNECTION, AssistantMsg.empty()))
                )
                // 将异常转为 ERROR 事件，让 SSE 流优雅结束，而非暴力断开连接
                .onErrorResume(error -> {
                    log.error("智能体流式处理异常: {}", error.getMessage(), error);
                    return Flux.just(HeadlessResponse.error(error.getMessage()));
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

    /**
     * 将 Spring AI 的 AssistantMessage 转换为自定义的 AssistantMsg
     */
    private AssistantMsg convertToAssistantMsg(AssistantMessage aiMessage) {
        List<ContentBlock> blocks = new ArrayList<>();

        String thinking = Optional.ofNullable(aiMessage.getMetadata().get(AgentScopeMessageUtils.REASONING_CONTENT_KEY))
                .map(Object::toString)
                .orElse(null);
        if (StringUtils.hasText(aiMessage.getText())) {
            blocks.add(TextBlock.builder().text(aiMessage.getText()).build());
        } else if (StringUtils.hasText(thinking)) {
            blocks.add(ThinkingBlock.builder()
                    .thinking(thinking)
                    .build());
        }

        List<Media> media = aiMessage.getMedia();
        if (!CollectionUtils.isEmpty(media)) {
            for (Media mediaItem : media) {
                String mimeType = mediaItem.getMimeType().toString();
                Object data = mediaItem.getData();
                String dataStr = data != null ? String.valueOf(data) : null;

                if (mimeType.startsWith("image")) {
                    blocks.add(ImageBlock.builder().url(dataStr).build());
                } else if (mimeType.startsWith("video")) {
                    blocks.add(VideoBlock.builder().url(dataStr).build());
                } else if (mimeType.startsWith("audio")) {
                    blocks.add(AudioBlock.builder().url(dataStr).build());
                }
            }
        }

        return AssistantMsg.builder()
                .content(blocks)
                .build();
    }
}
