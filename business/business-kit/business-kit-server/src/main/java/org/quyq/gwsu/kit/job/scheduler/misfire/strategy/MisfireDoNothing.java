package org.quyq.gwsu.kit.job.scheduler.misfire.strategy;

import org.quyq.gwsu.kit.job.scheduler.misfire.MisfireHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 忽略过期调度
 */
public class MisfireDoNothing extends MisfireHandler {
    private static final Logger logger = LoggerFactory.getLogger(MisfireDoNothing.class);

    @Override
    public void handle(int jobId) {
        logger.warn(">>>>>>>>>>> 任务调度过期忽略：jobId = {}", jobId);
    }

}
