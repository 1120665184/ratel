package org.quyq.gwsu.kit.job.scheduler.thread;

import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.mapper.KitJobInfoMapper;
import org.quyq.gwsu.kit.job.mapper.KitJobLogMapper;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.quyq.gwsu.kit.job.scheduler.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务失败告警监控助手
 */
public class JobFailAlarmMonitorHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobFailAlarmMonitorHelper.class);

    private ScheduledExecutorService monitorScheduler;

    /**
     * 启动
     */
    public void start() {
        monitorScheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "kit-job, admin JobFailAlarmMonitorHelper-monitorThread"));
        monitorScheduler.scheduleAtFixedRate(this::monitorTask,
                10, 10, TimeUnit.SECONDS);
    }

    /**
     * 监控任务
     */
    private void monitorTask() {
        try {
            List<String> failLogIds = JobAdminBootstrap.getInstance().getKitJobLogMapper().findFailJobLogIds(1000);
            if (failLogIds != null && !failLogIds.isEmpty()) {
                for (String failLogId : failLogIds) {

                    // 锁定日志
                    int lockRet = JobAdminBootstrap.getInstance().getKitJobLogMapper().updateAlarmStatus(failLogId, 0, -1);
                    if (lockRet < 1) {
                        continue;
                    }
                    KitJobLog log = JobAdminBootstrap.getInstance().getKitJobLogMapper().selectById(failLogId);
                    KitJobInfo info = JobAdminBootstrap.getInstance().getKitJobInfoMapper().selectById(log.getJobId());

                    // 1、失败重试监控
                    if (log.getExecutorFailRetryCount() > 0) {
                        JobAdminBootstrap.getInstance().getJobTriggerPoolHelper().trigger(log.getJobId(), TriggerTypeEnum.RETRY, (log.getExecutorFailRetryCount() - 1), log.getExecutorShardingParam(), log.getExecutorParam(), null);
                        String retryMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>失败重试<<<<<<<<<<<<< </span><br>";
                        log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
                        JobAdminBootstrap.getInstance().getKitJobLogMapper().updateTriggerInfo(log);
                    }

                    // 2、失败告警监控
                    int newAlarmStatus = 0;     // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
                    if (info != null) {
                        boolean alarmResult = JobAdminBootstrap.getInstance().getJobAlarmer().alarm(info, log);
                        newAlarmStatus = alarmResult ? 2 : 3;
                    } else {
                        newAlarmStatus = 1;
                    }

                    JobAdminBootstrap.getInstance().getKitJobLogMapper().updateAlarmStatus(failLogId, -1, newAlarmStatus);
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> kit-job, JobFailAlarmMonitorHelper#monitorTask error:{}", e.getMessage(), e);
        }
    }

    /**
     * 停止
     */
    public void stop() {
        monitorScheduler.shutdownNow();
    }

}
