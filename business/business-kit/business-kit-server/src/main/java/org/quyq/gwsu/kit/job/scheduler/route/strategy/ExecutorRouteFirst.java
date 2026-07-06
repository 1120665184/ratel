package org.quyq.gwsu.kit.job.scheduler.route.strategy;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouter;

import java.util.List;

/**
 * 第一个路由策略
 */
public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public R<String> route(TriggerRequest triggerParam, List<String> addressList) {
        return R.ok(addressList.get(0));
    }

}
