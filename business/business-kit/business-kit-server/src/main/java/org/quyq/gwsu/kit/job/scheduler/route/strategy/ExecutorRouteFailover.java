package org.quyq.gwsu.kit.job.scheduler.route.strategy;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouter;

import java.util.List;

/**
 * 故障转移路由策略
 */
public class ExecutorRouteFailover extends ExecutorRouter {

    @Override
    public R<String> route(TriggerRequest triggerParam, List<String> addressList) {

        StringBuilder beatResultSB = new StringBuilder();
        for (String address : addressList) {
            // 心跳检测
            R<String> beatResult = null;
            try {
                beatResult = JobAdminBootstrap.getInstance().getTriggerStrategy().beat(address);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                beatResult = R.fail(e.getMessage());
            }
            beatResultSB.append(beatResultSB.length() > 0 ? "<br><br>" : "")
                    .append("心跳检测：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.code())
                    .append("<br>msg：").append(beatResult.msg());

            // 心跳成功
            if (beatResult.isSuccess()) {
                return R.ok(address, beatResultSB.toString());
            }
        }

        return R.fail(beatResultSB.toString());
    }

}
