package org.quyq.gwsu.kit.job.scheduler.type.strategy;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleType;

import java.time.LocalDateTime;

/**
 * 无调度
 */
public class NoneScheduleType extends ScheduleType {

    @Override
    public LocalDateTime generateNextTriggerTime(KitJobInfo jobInfo, LocalDateTime fromTime) throws Exception {
        return null;
    }

}
