package org.quyq.gwsu.kit.job.scheduler.type;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;

import java.time.LocalDateTime;

/**
 * 调度类型抽象类
 */
public abstract class ScheduleType {

    /**
     * 生成下次触发时间
     *
     * @param jobInfo   任务信息
     * @param fromTime  起始时间
     * @return 下次触发时间
     */
    public abstract LocalDateTime generateNextTriggerTime(KitJobInfo jobInfo, LocalDateTime fromTime) throws Exception;

}
