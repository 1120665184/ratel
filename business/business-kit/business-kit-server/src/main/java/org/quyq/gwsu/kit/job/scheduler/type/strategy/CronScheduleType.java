package org.quyq.gwsu.kit.job.scheduler.type.strategy;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.scheduler.cron.CronExpression;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleType;

import java.util.Date;

/**
 * Cron调度
 */
public class CronScheduleType extends ScheduleType {

    @Override
    public Date generateNextTriggerTime(KitJobInfo jobInfo, Date fromTime) throws Exception {
        return new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromTime);
    }

}
