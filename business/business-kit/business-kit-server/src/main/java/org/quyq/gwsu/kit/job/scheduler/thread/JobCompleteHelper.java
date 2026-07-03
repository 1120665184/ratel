package org.quyq.gwsu.kit.job.scheduler.thread;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.common.job.openapi.admin.dto.CallbackData;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.mapper.KitJobLogMapper;
import org.quyq.gwsu.kit.job.scheduler.config.JobAdminBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * 任务完成助手（回调处理 + 丢失任务检测）
 */
public class JobCompleteHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class);

    // 回调线程池
    private ThreadPoolExecutor callbackThreadPool = null;

    // 丢失任务检测调度器
    private ScheduledExecutorService jobMonitorScheduler;

    /**
     * 启动
     */
    public void start() {

        // 1、回调线程池
        callbackThreadPool = new ThreadPoolExecutor(
                2,
                20,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(3000),
                r -> new Thread(r, "kit-job, admin JobCompleteHelper-callbackThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    logger.warn(">>>>>>>>>>> kit-job, 回调过快，触发拒绝策略（直接执行）。");
                });

        // 2、丢失任务检测线程
        jobMonitorScheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "kit-job, admin JobCompleteHelper-jobMonitorThread"));
        jobMonitorScheduler.scheduleAtFixedRate(this::jobMonitorTask,
                60, 60, TimeUnit.SECONDS);
    }

    /**
     * 丢失任务检测
     */
    private void jobMonitorTask() {
        try {
            // 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败
            Date losedTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
            List<Long> losedJobIds = JobAdminBootstrap.getInstance().getKitJobLogMapper().findLostJobIds(losedTime);

            if (losedJobIds != null && !losedJobIds.isEmpty()) {
                for (Long logId : losedJobIds) {
                    KitJobLog jobLog = new KitJobLog();
                    jobLog.setId(logId);
                    jobLog.setHandleTime(new Date());
                    jobLog.setHandleCode(JobConst.HANDLE_CODE_FAIL);
                    jobLog.setHandleMsg("任务结果丢失，标记失败");

                    JobAdminBootstrap.getInstance().getJobCompleter().complete(jobLog);
                }
            }
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> kit-job, JobCompleteHelper#jobMonitorTask error:{}", e.getMessage(), e);
        }
    }

    /**
     * 停止
     */
    public void stop() {
        callbackThreadPool.shutdownNow();
        jobMonitorScheduler.shutdownNow();
    }

    // ---------------------- 辅助方法 ----------------------

    /**
     * 回调
     *
     * @param callbackParamList 回调参数列表
     * @return 回调结果
     */
    public R<String> callback(List<CallbackData> callbackParamList) {

        callbackThreadPool.execute(() -> {
            for (CallbackData callbackRequest : callbackParamList) {
                R<String> callbackResult = doCallback(callbackRequest);
                logger.debug(">>>>>>>>> 任务回调 {}, callbackRequest={}, callbackResult={}",
                        (callbackResult.isSuccess() ? "成功" : "失败"), callbackRequest, callbackResult);
            }
        });

        return R.ok();
    }

    private R<String> doCallback(CallbackData handleCallbackParam) {
        // 校验日志
        KitJobLog log = JobAdminBootstrap.getInstance().getKitJobLogMapper().selectById(handleCallbackParam.getLogId());
        if (log == null) {
            return R.fail("日志记录未找到");
        }
        if (log.getHandleCode() > 0) {
            return R.fail("日志重复回调");
        }

        // 处理消息
        StringBuilder handleMsg = new StringBuilder();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getHandleMsg() != null) {
            handleMsg.append(handleCallbackParam.getHandleMsg());
        }

        // 保存日志
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getHandleCode());
        log.setHandleMsg(handleMsg.toString());
        JobAdminBootstrap.getInstance().getJobCompleter().complete(log);

        return R.ok();
    }

}
