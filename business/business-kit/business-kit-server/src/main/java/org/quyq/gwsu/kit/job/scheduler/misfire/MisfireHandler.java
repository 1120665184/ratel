package org.quyq.gwsu.kit.job.scheduler.misfire;

/**
 * 调度过期处理器
 */
public abstract class MisfireHandler {

    /**
     * 处理过期调度
     *
     * @param jobId 任务ID
     */
    public abstract void handle(final int jobId);

}
