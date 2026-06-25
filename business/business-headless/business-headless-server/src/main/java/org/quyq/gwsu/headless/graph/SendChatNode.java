package org.quyq.gwsu.headless.graph;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.agentscope.core.session.Session;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 普通消息发送节点
 */
@RequiredArgsConstructor
public class SendChatNode implements NodeAction {

    private final Session session;

    private final HeadlessBrowserManager headlessBrowserManager;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    @Override
    public Map<String, Object> apply(OverAllState state) {

        String query = state.value(HeadlessConstants.Headless.GRAPH_PARAM_QUERY, "");
        String userId = state.value(HeadlessConstants.Headless.GRAPH_PARAM_USER_ID, String.class).orElse("");

        HeadlessMessageHandler handler = new HeadlessMessageHandler(userId, session);

        executorService.submit(() -> {
            try {
                headlessBrowserManager.sendMessage(userId, query, handler);
                handler.complete();
            } catch (Exception e) {
                handler.error(e);
            }
        });


        return Map.of(HeadlessConstants.Headless.GRAPH_PARAM_OUTPUT, handler.asFlux()
                .startWith(Flux.just(
                        new ChatResponse(List.of(new Generation(
                                AssistantMessage.builder()
                                        .properties(Map.of("status", HeadlessAgentStatus.INITING))
                                        .content("")
                                        .build()
                        )))
                )));
    }
}
