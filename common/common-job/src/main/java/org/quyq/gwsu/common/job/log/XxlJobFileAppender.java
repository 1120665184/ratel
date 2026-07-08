package org.quyq.gwsu.common.job.log;

import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 任务日志文件追加器
 */
public class XxlJobFileAppender {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobFileAppender.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 日志基础路径
     * <p>
     * 结构如下：
     * ---/gluesource/10_1514171108000.js
     * ---/callbacklogs/xxl-job-callback-1761412677119.log
     * ---/2017-12-25/639.log
     * ---/2017-12-25/821.log
     */
    private static String logBasePath;
    private static String glueSrcPath;
    private static String callbackLogPath;

    /**
     * 初始化日志路径
     */
    public static void initLogPath(String logPath) throws IOException {
        if (logPath == null || logPath.trim().isEmpty()) {
            throw new RuntimeException("xxl-job logPath cannot be empty");
        }
        logBasePath = logPath.trim();

        // 创建基础目录
        File logPathDir = new File(logBasePath);
        Files.createDirectories(logPathDir.toPath());
        logBasePath = logPathDir.getPath();

        // 创建glue目录
        File glueBaseDir = new File(logPathDir, "gluesource");
        Files.createDirectories(glueBaseDir.toPath());
        glueSrcPath = glueBaseDir.getPath();

        // 创建回调日志目录
        File callbackBaseDir = new File(logPathDir, "callbacklogs");
        Files.createDirectories(callbackBaseDir.toPath());
        callbackLogPath = callbackBaseDir.getPath();
    }

    public static String getLogPath() {
        return logBasePath;
    }

    public static String getGlueSrcPath() {
        return glueSrcPath;
    }

    public static String getCallbackLogPath() {
        return callbackLogPath;
    }

    /**
     * 构建日志文件名，格式：logPath/yyyy-MM-dd/9999.log
     *
     * @param triggerDateTime 触发日期时间
     * @param logId           日志ID
     * @return 日志文件名
     */
    public static String makeLogFileName(LocalDateTime triggerDateTime, String logId) {
        String datePath = triggerDateTime.toLocalDate().format(DATE_FORMATTER);

        File logFilePath = new File(getLogPath(), datePath);
        try {
            Files.createDirectories(logFilePath.toPath());
        } catch (IOException e) {
            throw new RuntimeException("XxlJobFileAppender makeLogFileName error, logFilePath:" + logFilePath.getPath(), e);
        }

        return logFilePath.getPath()
                .concat(File.separator)
                .concat(String.valueOf(logId))
                .concat(".log");
    }

    /**
     * 追加日志
     *
     * @param logFileName 日志文件名
     * @param appendLog   日志内容
     */
    public static void appendLog(String logFileName, String appendLog) {
        if (logFileName == null || logFileName.trim().isEmpty() || appendLog == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(logFileName, true)) {
            writer.write(appendLog + System.lineSeparator());
        } catch (IOException e) {
            logger.error("XxlJobFileAppender appendLog error, logFileName:{}", logFileName, e);
        }
    }

    /**
     * 读取日志文件
     *
     * @param logFileName 日志文件名
     * @param fromLineNum 起始行号
     * @return 日志数据
     */
    public static LogData readLog(String logFileName, final int fromLineNum) {
        if (logFileName == null || logFileName.trim().isEmpty()) {
            return new LogData(fromLineNum, 0, "readLog fail, logFile not found", true);
        }

        Path path = Path.of(logFileName);
        if (!Files.exists(path)) {
            return new LogData(fromLineNum, 0, "readLog fail, logFile not exists", true);
        }

        StringBuilder logContentBuilder = new StringBuilder();
        AtomicInteger toLineNum = new AtomicInteger(0);
        AtomicInteger currentLineNum = new AtomicInteger(0);

        try (BufferedReader reader = new BufferedReader(new FileReader(logFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                currentLineNum.incrementAndGet();
                if (currentLineNum.get() < fromLineNum) {
                    continue;
                }
                toLineNum.set(currentLineNum.get());
                logContentBuilder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            logger.error("XxlJobFileAppender readLog error, logFileName:{}, fromLineNum:{}", logFileName, fromLineNum, e);
        }

        return new LogData(fromLineNum, toLineNum.get(), logContentBuilder.toString(), false);
    }

}
