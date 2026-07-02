package org.quyq.gwsu.kit.job.scheduler.alarm.impl;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.scheduler.alarm.JobAlarm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 钉钉任务告警（预留实现，当前仅日志输出）
 */
@Component
public class DingTalkJobAlarm implements JobAlarm {
    private static final Logger logger = LoggerFactory.getLogger(DingTalkJobAlarm.class);

    @Override
    public boolean doAlarm(KitJobInfo info, KitJobLog jobLog) {
        // 预留钉钉告警实现，当前仅日志输出
        logger.warn(">>>>>>>>>>> 任务失败告警：jobId={}, logId={}, handleMsg={}",
                info != null ? info.getId() : null,
                jobLog != null ? jobLog.getId() : null,
                jobLog != null ? jobLog.getHandleMsg() : null);
        return true;
    }

}
