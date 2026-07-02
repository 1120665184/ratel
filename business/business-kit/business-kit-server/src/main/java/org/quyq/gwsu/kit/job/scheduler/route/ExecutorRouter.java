package org.quyq.gwsu.kit.job.scheduler.route;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 执行器路由抽象类
 */
public abstract class ExecutorRouter {
    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);

    /**
     * 路由地址
     *
     * @param triggerRequest 触发请求
     * @param jobGroup       执行器组
     * @return R.content=address
     */
    public abstract R<String> route(TriggerRequest triggerRequest, KitJobGroup jobGroup);

}
