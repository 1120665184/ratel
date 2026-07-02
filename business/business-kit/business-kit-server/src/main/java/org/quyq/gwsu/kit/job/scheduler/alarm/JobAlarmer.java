package org.quyq.gwsu.kit.job.scheduler.alarm;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务告警器
 */
@Component
public class JobAlarmer {
    private static final Logger logger = LoggerFactory.getLogger(JobAlarmer.class);

    @Autowired
    private List<JobAlarm> jobAlarmList;

    /**
     * 任务告警
     */
    public boolean alarm(KitJobInfo info, KitJobLog jobLog) {

        boolean result = false;
        if (jobAlarmList != null && !jobAlarmList.isEmpty()) {
            result = true;  // 所有告警器都成功才算成功
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doAlarm(info, jobLog);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }

        return result;
    }

}
