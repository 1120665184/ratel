package org.quyq.gwsu.kit.job.scheduler.type.strategy;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.scheduler.cron.CronExpression;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Cron调度
 */
public class CronScheduleType extends ScheduleType {

    @Override
    public LocalDateTime generateNextTriggerTime(KitJobInfo jobInfo, LocalDateTime fromTime) throws Exception {
        Date fromDate = Date.from(fromTime.atZone(ZoneId.systemDefault()).toInstant());
        Date nextValidTime = new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromDate);
        if (nextValidTime == null) {
            return null;
        }
        return nextValidTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}
