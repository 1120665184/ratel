package org.quyq.gwsu.common.job.thread;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.common.job.context.XxlJobContext;
import org.quyq.gwsu.common.job.context.XxlJobHelper;
import org.quyq.gwsu.common.job.executor.XxlJobExecutor;
import org.quyq.gwsu.common.job.log.XxlJobFileAppender;
import org.quyq.gwsu.common.job.openapi.admin.dto.CallbackData;
import org.quyq.gwsu.common.job.openapi.admin.dto.CallbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * 触发回调辅助类
 * <p>
 * 使用BlockingQueue + ScheduledExecutorService替代MessageQueue + CyclicThread
 */
public class TriggerCallbackHelper {
    private static final Logger logger = LoggerFactory.getLogger(TriggerCallbackHelper.class);

    private static final Gson gson = new Gson();
    private static final Type CALLBACK_LIST_TYPE = new TypeToken<List<CallbackData>>() {}.getType();

    /**
     * 回调消息队列
     */
    private volatile BlockingQueue<CallbackData> callbackQueue;

    /**
     * 回调执行线程
     */
    private ScheduledExecutorService callbackScheduler;

    /**
     * 重试回调文件线程
     */
    private ScheduledExecutorService retryCallbackScheduler;

    /**
     * 启动
     */
    public void start(final XxlJobExecutor xxlJobExecutor) {

        // 初始化回调队列
        callbackQueue = new LinkedBlockingQueue<>();

        // 1、回调线程：定时从队列取数据并回调
        callbackScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TriggerCallbackHelper#callbackThread");
            t.setDaemon(true);
            return t;
        });

        callbackScheduler.scheduleAtFixedRate(() -> {
            try {
                List<CallbackData> callbackDataList = new ArrayList<>();
                callbackQueue.drainTo(callbackDataList, 50);
                if (!callbackDataList.isEmpty()) {
                    doCallback(callbackDataList, xxlJobExecutor);
                }
            } catch (Exception e) {
                logger.error("TriggerCallbackHelper callback error", e);
            }
        }, 1, 1, TimeUnit.SECONDS);

        // 2、重试回调文件线程
        retryCallbackScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TriggerCallbackHelper#retryCallbackThread");
            t.setDaemon(true);
            return t;
        });

        retryCallbackScheduler.scheduleAtFixedRate(() -> {
            try {
                retryFailCallbackFile(xxlJobExecutor);
            } catch (Exception e) {
                logger.error("TriggerCallbackHelper retryFailCallbackFile error", e);
            }
        }, JobConst.REGISTRY_BEAT_INTERVAL, JobConst.REGISTRY_BEAT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * 停止
     */
    public void stop() {
        // 1、停止回调队列处理（先处理完剩余）
        if (callbackQueue != null && !callbackQueue.isEmpty()) {
            List<CallbackData> remaining = new ArrayList<>();
            callbackQueue.drainTo(remaining);
            if (!remaining.isEmpty()) {
                doCallback(remaining, XxlJobExecutor.getInstance());
            }
        }

        // 2、停止调度器
        if (callbackScheduler != null) {
            callbackScheduler.shutdown();
        }
        if (retryCallbackScheduler != null) {
            retryCallbackScheduler.shutdown();
        }
    }

    /**
     * 提交回调消息
     */
    public void pushCallBack(CallbackData callback) {
        if (!callbackQueue.offer(callback)) {
            doCallback(new ArrayList<>(Collections.singletonList(callback)), XxlJobExecutor.getInstance());
        }
        logger.debug(">>>>>>>>>>> xxl-job, push callback request, logId:{}", callback.getLogId());
    }

    // ---------------------- 执行回调 ----------------------

    /**
     * 执行回调，失败会重试
     */
    private void doCallback(List<CallbackData> callbackDataList, final XxlJobExecutor xxlJobExecutor) {
        boolean callbackRet = false;

        try {
            R<String> callbackResult = xxlJobExecutor.getJobAdminClientApi().callback(new CallbackRequest(callbackDataList));
            if (callbackResult != null && callbackResult.isSuccess()) {
                appendCallbackResult(callbackDataList, "<br>----------- xxl-job job callback finish.");
                callbackRet = true;
            } else {
                appendCallbackResult(callbackDataList, "<br>----------- xxl-job job callback fail, callbackResult:" + callbackResult);
            }
        } catch (Throwable e) {
            appendCallbackResult(callbackDataList, "<br>----------- xxl-job job callback error, errorMsg:" + e.getMessage());
        }

        // 回调失败，写文件稍后重试
        if (!callbackRet) {
            writeCallbackLog(callbackDataList);
        }
    }

    /**
     * 追加回调结果到每个任务日志
     */
    private void appendCallbackResult(List<CallbackData> callbackParamList, String logContent) {
        for (CallbackData callbackParam : callbackParamList) {
            String logFileName = XxlJobFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTime()), callbackParam.getLogId());
            XxlJobContext.setXxlJobContext(new XxlJobContext(
                    "",
                    null,
                    "",
                    -1,
                    logFileName,
                    -1,
                    -1));
            XxlJobHelper.log(logContent);
        }
    }

    // ---------------------- 失败回调文件 ----------------------

    /**
     * 失败回调文件名模板
     */
    private static final String failCallbackFileName = XxlJobFileAppender
            .getCallbackLogPath()
            .concat(File.separator)
            .concat("xxl-job-callback-{x}")
            .concat(".log");

    /**
     * 写入失败回调文件，稍后重试
     */
    private void writeCallbackLog(List<CallbackData> callbackParamList) {
        if (callbackParamList == null || callbackParamList.isEmpty()) {
            return;
        }

        String callbackData = gson.toJson(callbackParamList);
        String callbackDataMd5 = md5(callbackData);

        String finalLogFileName = failCallbackFileName.replace("{x}", callbackDataMd5);

        try {
            Files.writeString(Path.of(finalLogFileName), callbackData);
        } catch (IOException e) {
            logger.error(">>>>>>>>>>> TriggerCallbackHelper writeCallbackLog error, finalLogFileName:{}", finalLogFileName, e);
        }
    }

    /**
     * 重试失败的回调文件
     */
    private void retryFailCallbackFile(final XxlJobExecutor xxlJobExecutor) {
        File callbackLogPath = new File(XxlJobFileAppender.getCallbackLogPath());
        if (!callbackLogPath.exists()) {
            return;
        }
        if (!callbackLogPath.isDirectory()) {
            callbackLogPath.delete();
            return;
        }
        File[] files = callbackLogPath.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (File callbackLogFile : files) {
            try {
                String callbackData = Files.readString(callbackLogFile.toPath());
                if (callbackData == null || callbackData.isEmpty()) {
                    callbackLogFile.delete();
                    continue;
                }

                List<CallbackData> callbackParamList = gson.fromJson(callbackData, CALLBACK_LIST_TYPE);
                callbackLogFile.delete();

                doCallback(callbackParamList, xxlJobExecutor);
            } catch (IOException e) {
                logger.error(">>>>>>>>>>> TriggerCallbackHelper retryFailCallbackFile error, callbackLogFile:{}", callbackLogFile.getPath(), e);
            }
        }
    }

    /**
     * MD5摘要
     */
    private static String md5(String input) {
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : md5) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

}
