package org.quyq.gwsu.kit.job.scheduler.type.strategy;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.scheduler.type.ScheduleType;

import java.util.Date;

/**
 * 固定速率调度
 */
public class FixRateScheduleType extends ScheduleType {

    @Override
    public Date generateNextTriggerTime(KitJobInfo jobInfo, Date fromTime) throws Exception {
        // 根据固定速率（秒）生成下次触发时间
        Date nextTriggerTime = new Date(fromTime.getTime() + Long.parseLong(jobInfo.getScheduleConf()) * 1000L);

        // 对齐到秒
        if (nextTriggerTime.getTime() % 1000 != 0) {
            nextTriggerTime = new Date((nextTriggerTime.getTime() / 1000 + 1) * 1000);
        }

        return nextTriggerTime;
    }

}
