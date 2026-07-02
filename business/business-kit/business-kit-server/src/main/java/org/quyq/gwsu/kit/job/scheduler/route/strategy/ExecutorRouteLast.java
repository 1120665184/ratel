package org.quyq.gwsu.kit.job.scheduler.route.strategy;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouter;

/**
 * 最后一个路由策略
 */
public class ExecutorRouteLast extends ExecutorRouter {

    @Override
    public R<String> route(TriggerRequest triggerParam, KitJobGroup jobGroup) {
        return R.ok(jobGroup.getRegistryList().get(jobGroup.getRegistryList().size() - 1));
    }

}
