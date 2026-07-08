package org.quyq.gwsu.kit.job.scheduler.alarm;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;

/**
 * 任务告警接口
 */
public interface JobAlarm {

    /**
     * 任务告警
     *
     * @param info   任务信息
     * @param jobLog 任务日志
     * @return 是否告警成功
     */
    boolean doAlarm(KitJobInfo info, KitJobLog jobLog);

}
