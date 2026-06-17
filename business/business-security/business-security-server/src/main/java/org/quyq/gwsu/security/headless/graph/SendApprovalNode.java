package org.quyq.gwsu.security.headless.graph;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.security.constants.SerConstants;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.quyq.gwsu.security.headless.domain.RouterInfo;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 审批消息发送节点
 */
@RequiredArgsConstructor
public class SendApprovalNode implements NodeAction {

    private final HeadlessBrowserManager headlessBrowserManager;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    @Override
    public Map<String, Object> apply(OverAllState state) {

        RouterInfo routerInfo = state.value(SerConstants.Headless.GRAPH_PARAM_ROUTE_INFO, RouterInfo.class).orElse(null);
        AssertUtils.notNull(routerInfo, SecurityErrorCode.E07001);

        RouterInfo.ApprovalInfo approvalInfo = routerInfo.getApprovalInfo();
        AssertUtils.notNull(approvalInfo, SecurityErrorCode.E07002);

        String userId = state.value(SerConstants.Headless.GRAPH_PARAM_USER_ID, String.class).orElse("");

        HeadlessMessageHandler handler = new HeadlessMessageHandler();

        //发送审批消息
        executorService.submit(() -> {
            headlessBrowserManager.approval(
                    userId,
                    approvalInfo.agree(), approvalInfo.refuseReason(),
                    handler
            );
            handler.complete();
        });


        return Map.of(SerConstants.Headless.GRAPH_PARAM_OUTPUT, handler.asFlux());

    }
}
