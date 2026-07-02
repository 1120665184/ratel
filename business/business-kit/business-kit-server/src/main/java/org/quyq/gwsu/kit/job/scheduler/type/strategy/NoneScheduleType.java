package org.quyq.gwsu.kit.job.scheduler.type.strategy;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleType;

import java.util.Date;

/**
 * 无调度
 */
public class NoneScheduleType extends ScheduleType {

    @Override
    public Date generateNextTriggerTime(KitJobInfo jobInfo, Date fromTime) throws Exception {
        return null;
    }

}
