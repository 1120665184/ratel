package org.quyq.gwsu.kit.job.scheduler.type.strategy;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleType;

import java.time.LocalDateTime;

/**
 * 固定速率调度
 */
public class FixRateScheduleType extends ScheduleType {

    @Override
    public LocalDateTime generateNextTriggerTime(KitJobInfo jobInfo, LocalDateTime fromTime) throws Exception {
        // 根据固定速率（秒）生成下次触发时间
        long addSeconds = Long.parseLong(jobInfo.getScheduleConf());
        return fromTime.plusSeconds(addSeconds);
    }

}
