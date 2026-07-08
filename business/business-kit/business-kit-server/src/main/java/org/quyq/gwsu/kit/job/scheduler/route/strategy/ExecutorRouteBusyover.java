package org.quyq.gwsu.kit.job.scheduler.route.strategy;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.IdleBeatRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouter;

import java.util.List;

/**
 * 忙碌转移路由策略
 */
public class ExecutorRouteBusyover extends ExecutorRouter {

    @Override
    public R<String> route(TriggerRequest triggerParam, List<String> addressList) {

        StringBuilder idleBeatResultSB = new StringBuilder();
        for (String address : addressList) {
            // 空闲检测
            R<String> idleBeatResult = null;
            try {
                idleBeatResult = JobAdminBootstrap.getInstance().getTriggerStrategy()
                        .idleBeat(address, new IdleBeatRequest(triggerParam.getJobId()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                idleBeatResult = R.fail("" + e);
            }
            idleBeatResultSB.append(idleBeatResultSB.length() > 0 ? "<br><br>" : "")
                    .append("空闲检测：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(idleBeatResult.code())
                    .append("<br>msg：").append(idleBeatResult.msg());

            // 空闲成功
            if (idleBeatResult.isSuccess()) {
                return R.ok(address, idleBeatResultSB.toString());
            }
        }

        return R.fail(idleBeatResultSB.toString());
    }

}
