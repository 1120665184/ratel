package org.quyq.gwsu.common.job.context;

import org.quyq.gwsu.common.job.log.XxlJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 任务辅助工具类
 */
public class XxlJobHelper {

    // ---------------------- 任务信息 ----------------------

    /**
     * 获取当前任务ID
     */
    public static long getJobId() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getJobId();
    }

    /**
     * 获取当前任务参数
     */
    public static String getJobParam() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }
        return xxlJobContext.getJobParam();
    }

    // ---------------------- 日志信息 ----------------------

    /**
     * 获取当前日志ID
     */
    public static long getLogId() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getLogId();
    }

    /**
     * 获取当前日志时间戳
     */
    public static long getLogDateTime() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getLogDateTime();
    }

    /**
     * 获取当前日志文件名
     */
    public static String getLogFileName() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }
        return xxlJobContext.getLogFileName();
    }

    // ---------------------- 分片信息 ----------------------

    /**
     * 获取当前分片序号
     */
    public static int getShardIndex() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getShardIndex();
    }

    /**
     * 获取当前分片总数
     */
    public static int getShardTotal() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getShardTotal();
    }

    // ---------------------- 日志工具 ----------------------

    private static final Logger logger = LoggerFactory.getLogger("xxl-job logger");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 追加日志（带格式化）
     *
     * @param appendLogPattern    日志模板，如 "aaa {} bbb {} ccc"
     * @param appendLogArguments  日志参数，如 "111, true"
     */
    public static boolean log(String appendLogPattern, Object... appendLogArguments) {

        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * 追加异常堆栈日志
     *
     * @param e 异常
     * @return 是否成功
     */
    public static boolean log(Throwable e) {

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * 追加日志详情
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        String formatAppendLog = LocalDateTime.now().format(DATE_TIME_FORMATTER) + " " +
                "[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]" + "-" +
                "[" + callInfo.getLineNumber() + "]" + "-" +
                "[" + Thread.currentThread().getName() + "]" + " " +
                (appendLog != null ? appendLog : "");

        String logFileName = xxlJobContext.getLogFileName();

        if (logFileName != null && !logFileName.trim().isEmpty()) {
            XxlJobFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            logger.info(">>>>>>>>>>> {}", formatAppendLog);
            return false;
        }
    }

    // ---------------------- 处理结果工具 ----------------------

    /**
     * 处理成功
     */
    public static boolean handleSuccess() {
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, null);
    }

    /**
     * 处理成功（带消息）
     */
    public static boolean handleSuccess(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, handleMsg);
    }

    /**
     * 处理失败
     */
    public static boolean handleFail() {
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, null);
    }

    /**
     * 处理失败（带消息）
     */
    public static boolean handleFail(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, handleMsg);
    }

    /**
     * 处理超时
     */
    public static boolean handleTimeout() {
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, null);
    }

    /**
     * 处理超时（带消息）
     */
    public static boolean handleTimeout(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, handleMsg);
    }

    /**
     * 设置处理结果
     *
     * @param handleCode 处理码：200=成功, 500=失败, 502=超时
     * @param handleMsg  处理消息
     * @return 是否成功
     */
    public static boolean handleResult(int handleCode, String handleMsg) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        xxlJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            xxlJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }

}
