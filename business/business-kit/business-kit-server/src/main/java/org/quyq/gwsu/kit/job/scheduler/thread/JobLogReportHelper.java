package org.quyq.gwsu.kit.job.scheduler.thread;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.quyq.gwsu.kit.job.domain.KitJobLogReport;
import org.quyq.gwsu.kit.job.mapper.KitJobLogMapper;
import org.quyq.gwsu.kit.job.mapper.KitJobLogReportMapper;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务日志报表助手
 */
public class JobLogReportHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobLogReportHelper.class);

    private ScheduledExecutorService logReportScheduler;
    private AtomicLong lastCleanLogTime;

    /**
     * 启动
     */
    public void start() {
        lastCleanLogTime = new AtomicLong(0);

        logReportScheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "kit-job, admin JobLogReportHelper-logReportThread"));
        logReportScheduler.scheduleAtFixedRate(this::logReportTask,
                60, 60, TimeUnit.SECONDS);
    }

    /**
     * 日志报表任务
     */
    private void logReportTask() {
        try {
            // 1、日志报表刷新：刷新3天内的日志报表
            for (int i = 0; i < 3; i++) {

                LocalDate day = LocalDate.now().minusDays(i);
                LocalDateTime todayFrom = LocalDateTime.of(day, LocalTime.MIN);
                LocalDateTime todayTo = LocalDateTime.of(day, LocalTime.MAX);

                // 刷新日志报表
                KitJobLogReport kitJobLogReport = new KitJobLogReport();
                kitJobLogReport.setTriggerDay(todayFrom);
                kitJobLogReport.setRunningCount(0);
                kitJobLogReport.setSucCount(0);
                kitJobLogReport.setFailCount(0);

                // 填充统计数据
                Map<String, Object> triggerCountMap = JobAdminBootstrap.getInstance().getKitJobLogMapper().findLogReport(todayFrom, todayTo);
                if (triggerCountMap != null && !triggerCountMap.isEmpty()) {
                    int triggerDayCount = triggerCountMap.containsKey("triggerDayCount") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCount"))) : 0;
                    int triggerDayCountRunning = triggerCountMap.containsKey("triggerDayCountRunning") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCountRunning"))) : 0;
                    int triggerDayCountSuc = triggerCountMap.containsKey("triggerDayCountSuc") ? Integer.parseInt(String.valueOf(triggerCountMap.get("triggerDayCountSuc"))) : 0;
                    int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;

                    kitJobLogReport.setRunningCount(triggerDayCountRunning);
                    kitJobLogReport.setSucCount(triggerDayCountSuc);
                    kitJobLogReport.setFailCount(triggerDayCountFail);
                }

                // 执行刷新
                kitJobLogReport.setId(IdWorker.getIdStr());
                JobAdminBootstrap.getInstance().getKitJobLogReportMapper().saveOrUpdate(kitJobLogReport);
            }

            // 2、日志清理：每天执行一次
            if (JobAdminBootstrap.getInstance().getLogretentiondays() > 0
                    && System.currentTimeMillis() - lastCleanLogTime.longValue() > 24 * 60 * 60 * 1000) {

                LocalDateTime clearBeforeTime = LocalDate.now()
                        .minusDays(JobAdminBootstrap.getInstance().getLogretentiondays())
                        .atStartOfDay();

                // 清理过期日志
                List<String> logIds;
                do {
                    logIds = JobAdminBootstrap.getInstance().getKitJobLogMapper().findClearLogIds(null, null, clearBeforeTime, 0, 1000);
                    if (logIds != null && !logIds.isEmpty()) {
                        JobAdminBootstrap.getInstance().getKitJobLogMapper().clearLog(logIds);
                    }
                } while (logIds != null && !logIds.isEmpty());

                lastCleanLogTime.set(System.currentTimeMillis());
            }
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> kit-job, JobLogReportHelper#logReportTask error:{}", e.getMessage(), e);
        }
    }

    /**
     * 停止
     */
    public void stop() {
        logReportScheduler.shutdownNow();
    }

}
