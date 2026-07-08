package org.quyq.gwsu.kit.job.scheduler.misfire.strategy;

import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.misfire.MisfireHandler;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 立即执行一次过期调度
 */
public class MisfireFireOnceNow extends MisfireHandler {
    protected static Logger logger = LoggerFactory.getLogger(MisfireFireOnceNow.class);

    @Override
    public void handle(String jobId) {
        JobAdminBootstrap.getInstance().getJobTriggerPoolHelper().trigger(jobId, TriggerTypeEnum.MISFIRE, -1, null, null, null);
        logger.warn(">>>>>>>>>>> 任务调度过期立即执行：jobId = {}", jobId);
    }

}
