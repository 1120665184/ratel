package org.quyq.gwsu.headless.graph;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.domain.RouterInfo;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import org.quyq.gwsu.headless.errcode.HeadlessErrorCode;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 发送用户问题回复节点
 */
@RequiredArgsConstructor
public class SendAnswerNode implements NodeAction {

    private final AgentStateStore agentStateStore;

    private final HeadlessBrowserManager headlessBrowserManager;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    @Override
    public Map<String, Object> apply(OverAllState state) {
        RouterInfo routerInfo = state.value(HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO, RouterInfo.class).orElse(null);
        AssertUtils.notNull(routerInfo, HeadlessErrorCode.E01001);

        Map<String, String> answerInfo = routerInfo.getAnswerInfo();
        AssertUtils.notNull(answerInfo, HeadlessErrorCode.E01003);
        String toolCallId = routerInfo.getToolCallId();
        AssertUtils.hasText(toolCallId, HeadlessErrorCode.E01004);

        SubjectInfo userId = state.value(HeadlessConstants.Headless.GRAPH_PARAM_USER_ID, SubjectInfo.class).orElseThrow();
        HeadlessDTO request = state.value(HeadlessConstants.Headless.GRAPH_PARAM_REQUEST, HeadlessDTO.class).orElseThrow();
        String threadId = state.value(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, String.class).orElse("");

        HeadlessMessageHandler handler = new HeadlessMessageHandler(
                userId.userId(),
                agentStateStore,
                threadId,
                request.enableOutputPanelScreenshot(),
                request.enableApprovalRecording()
        );
        //发送用户回复
        executorService.submit(() -> {
            try {
                headlessBrowserManager.userAnswer(
                        userId,
                        toolCallId,
                        answerInfo,
                        handler
                );
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
