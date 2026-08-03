package org.quyq.gwsu.headless.graph;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 普通消息发送节点
 */
@RequiredArgsConstructor
public class SendChatNode implements NodeAction {

    private final AgentStateStore agentStateStore;

    private final HeadlessBrowserManager headlessBrowserManager;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    @Override
    public Map<String, Object> apply(OverAllState state) {

        HeadlessDTO request = state.value(HeadlessConstants.Headless.GRAPH_PARAM_REQUEST, HeadlessDTO.class).orElseThrow();
        SubjectInfo userId = state.value(HeadlessConstants.Headless.GRAPH_PARAM_USER_ID, SubjectInfo.class).orElseThrow();
        String threadId = state.value(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, String.class).orElse("");

        HeadlessMessageHandler handler = new HeadlessMessageHandler(
                userId.userId(),
                agentStateStore,
                threadId,
                request.enableOutputPanelScreenshot(),
                request.enableApprovalRecording()
        );

        executorService.submit(() -> {
            try {
                headlessBrowserManager.sendMessage(userId, request, handler);
                handler.complete();
            } catch (Exception e) {
                handler.error(e);
            }
        });


        return Map.of(HeadlessConstants.Headless.GRAPH_PARAM_OUTPUT, handler.asFlux()
                .startWith(Flux.just(HeadlessAguiEventBridge.toChatResponse(
                        HeadlessAguiEventBridge.statusEvent(threadId, "", org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus.INITING)
                ))));
    }
}
