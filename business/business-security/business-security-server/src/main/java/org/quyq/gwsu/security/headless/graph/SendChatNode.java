package org.quyq.gwsu.security.headless.graph;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.security.constants.SerConstants;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 普通消息发送节点
 */
@RequiredArgsConstructor
public class SendChatNode implements NodeAction {

    private final HeadlessBrowserManager headlessBrowserManager;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    @Override
    public Map<String, Object> apply(OverAllState state) {

        String query = state.value(SerConstants.Headless.GRAPH_PARAM_QUERY, "");
        String userId = state.value(SerConstants.Headless.GRAPH_PARAM_USER_ID, String.class).orElse("");

        HeadlessMessageHandler handler = new HeadlessMessageHandler();

        executorService.submit(() -> {
            headlessBrowserManager.sendMessage(userId, query, handler);
            handler.complete();
        });


        return Map.of(SerConstants.Headless.GRAPH_PARAM_OUTPUT, handler.asFlux());
    }
}
