package org.quyq.gwsu.common.job.thread;

import org.quyq.gwsu.common.job.log.XxlJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 日志文件清理辅助类
 * <p>
 * 使用ScheduledExecutorService替代CyclicThread
 */
public class JobLogFileCleanHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobLogFileCleanHelper.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 清理线程
     */
    private ScheduledExecutorService logFileCleanScheduler;

    /**
     * 启动
     */
    public void start(final long logRetentionDays) {

        // 最小值限制
        if (logRetentionDays < 3) {
            return;
        }

        logFileCleanScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JobLogFileCleanHelper#logFileCleanThread");
            t.setDaemon(true);
            return t;
        });

        logFileCleanScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanLogFile(logRetentionDays);
            } catch (Exception e) {
                logger.error("JobLogFileCleanHelper error", e);
            }
        }, 0, 24, TimeUnit.HOURS);
    }

    /**
     * 清理过期日志文件
     */
    private void cleanLogFile(long logRetentionDays) {
        File[] childDirs = new File(XxlJobFileAppender.getLogPath()).listFiles();
        if (childDirs == null || childDirs.length == 0) {
            return;
        }

        LocalDate today = LocalDate.now();

        for (File childFile : childDirs) {
            if (!childFile.isDirectory()) {
                continue;
            }

            if (!childFile.getName().contains("-")) {
                continue;
            }

            LocalDate logFileDate = null;
            try {
                logFileDate = LocalDate.parse(childFile.getName(), DATE_FORMATTER);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            if (logFileDate == null) {
                continue;
            }

            long daysBetween = ChronoUnit.DAYS.between(logFileDate, today);
            if (daysBetween >= logRetentionDays) {
                deleteDirectory(childFile);
            }
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * 停止
     */
    public void stop() {
        if (logFileCleanScheduler != null) {
            logFileCleanScheduler.shutdown();
        }
    }

}
