package org.quyq.gwsu.security.headless.service.impl;


import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import io.agentscope.core.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.constants.SerConstants;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.quyq.gwsu.security.headless.domain.HeadlessCallConfig;
import org.quyq.gwsu.security.headless.domain.HeadlessResponse;
import org.quyq.gwsu.security.headless.domain.RouterInfo;
import org.quyq.gwsu.security.headless.enums.HeadlessAgentStatus;
import org.quyq.gwsu.security.headless.graph.IntentRecognitionNode;
import org.quyq.gwsu.security.headless.graph.SendAnswerNode;
import org.quyq.gwsu.security.headless.graph.SendApprovalNode;
import org.quyq.gwsu.security.headless.graph.SendChatNode;
import org.quyq.gwsu.security.headless.service.IHeadlessService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeadlessServiceImpl implements IHeadlessService, InitializingBean {

    private final Session session;

    private final HeadlessBrowserManager headlessBrowserManager;

    private final SecurityUtils securityUtils;

    public CompiledGraph headlessGraph;


    @Override
    public Flux<HeadlessResponse> stream(String query, HeadlessCallConfig config) {

        AssertUtils.hasText(query, SecurityErrorCode.E07006);
        String userId = config.getUserId();
        if (!StringUtils.hasText(userId)) {
            userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse("");
        }
        AssertUtils.hasText(userId, SecurityErrorCode.E07005);

        return headlessGraph.stream(Map.of(
                        SerConstants.Headless.GRAPH_PARAM_QUERY, query,
                        SerConstants.Headless.GRAPH_PARAM_USER_ID, userId,
                        SerConstants.Headless.GRAPH_PARAM_THREAD_ID, Optional.ofNullable(config.getThreadId()).orElse("")
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
                    Message message = streamingOutput.message();
                    HeadlessAgentStatus status = (HeadlessAgentStatus) message.getMetadata().get("status");
                    message.getMetadata().remove("status");
                    return Flux.just(new HeadlessResponse(status, message));
                }).startWith(
                        Flux.just(new HeadlessResponse(HeadlessAgentStatus.CONNECTION, AssistantMessage.builder().content("").build()))
                );
    }


    public CompiledGraph buildGraph() throws GraphStateException {

        IntentRecognitionNode intentRecognitionNode = new IntentRecognitionNode(session, headlessBrowserManager);
        SendChatNode sendChatNode = new SendChatNode(session, headlessBrowserManager);
        SendAnswerNode sendAnswerNode = new SendAnswerNode(session, headlessBrowserManager);
        SendApprovalNode sendApprovalNode = new SendApprovalNode(session, headlessBrowserManager);

        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();

            keyStrategyMap.put(SerConstants.Headless.GRAPH_PARAM_QUERY, new ReplaceStrategy());
            keyStrategyMap.put(SerConstants.Headless.GRAPH_PARAM_THREAD_ID, new ReplaceStrategy());
            keyStrategyMap.put(SerConstants.Headless.GRAPH_PARAM_USER_ID, new ReplaceStrategy());
            keyStrategyMap.put(SerConstants.Headless.GRAPH_PARAM_ROUTE_INFO, new ReplaceStrategy());
            keyStrategyMap.put(SerConstants.Headless.GRAPH_PARAM_OUTPUT, new ReplaceStrategy());

            return keyStrategyMap;

        };

        StateGraph graph = new StateGraph(keyStrategyFactory)
                .addNode("intentRecognitionNode", AsyncNodeAction.node_async(intentRecognitionNode))
                .addNode("sendChatNode", AsyncNodeAction.node_async(sendChatNode))
                .addNode("sendAnswerNode", AsyncNodeAction.node_async(sendAnswerNode))
                .addNode("sendApprovalNode", AsyncNodeAction.node_async(sendApprovalNode))
                .addEdge(StateGraph.START, "intentRecognitionNode")
                .addConditionalEdges("intentRecognitionNode", AsyncEdgeAction.edge_async(state ->
                                state.value(SerConstants.Headless.GRAPH_PARAM_ROUTE_INFO, RouterInfo.class)
                                        .map(v -> v.getType().name()).orElse("")
                        ),
                        Map.of(
                                "", StateGraph.END,
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
}
