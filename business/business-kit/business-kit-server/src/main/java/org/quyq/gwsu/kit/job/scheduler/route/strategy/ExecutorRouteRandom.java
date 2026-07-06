package org.quyq.gwsu.kit.job.scheduler.route.strategy;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.scheduler.route.ExecutorRouter;

import java.util.List;
import java.util.Random;

/**
 * 随机路由策略
 */
public class ExecutorRouteRandom extends ExecutorRouter {

    private static final Random localRandom = new Random();

    @Override
    public R<String> route(TriggerRequest triggerParam, List<String> addressList) {
        String address = addressList.get(localRandom.nextInt(addressList.size()));
        return R.ok(address);
    }

}
