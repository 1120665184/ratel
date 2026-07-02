package org.quyq.gwsu.common.job.thread;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.common.job.context.XxlJobContext;
import org.quyq.gwsu.common.job.context.XxlJobHelper;
import org.quyq.gwsu.common.job.executor.XxlJobExecutor;
import org.quyq.gwsu.common.job.handler.IJobHandler;
import org.quyq.gwsu.common.job.log.XxlJobFileAppender;
import org.quyq.gwsu.common.job.openapi.admin.dto.CallbackData;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行线程
 */
public class JobThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(JobThread.class);

    private final int jobId;
    private final IJobHandler handler;
    private final LinkedBlockingQueue<TriggerRequest> triggerQueue;
    private final Set<Long> triggerLogIdSet;                        // 避免同一TRIGGER_LOG_ID重复触发

    private volatile boolean toStop = false;
    private String stopReason;

    private boolean running = false;
    private int idleTimes = 0;


    public JobThread(int jobId, IJobHandler handler) {
        this.jobId = jobId;
        this.handler = handler;
        this.triggerQueue = new LinkedBlockingQueue<>();
        this.triggerLogIdSet = ConcurrentHashMap.newKeySet();

        this.setName("xxl-job, JobThread-" + jobId + "-" + System.currentTimeMillis());
    }

    public IJobHandler getHandler() {
        return handler;
    }

    /**
     * 新触发请求加入队列
     */
    public R<String> pushTriggerQueue(TriggerRequest triggerParam) {
        // 避免重复
        if (!triggerLogIdSet.add(triggerParam.getLogId())) {
            logger.info(">>>>>>>>>>> repeat trigger job, logId:{}", triggerParam.getLogId());
            return new R<>(XxlJobContext.HANDLE_CODE_FAIL, "repeat trigger job, logId:" + triggerParam.getLogId(), null, null);
        }

        triggerQueue.add(triggerParam);
        return R.ok();
    }

    /**
     * 终止任务线程
     */
    public void toStop(String stopReason) {
        this.toStop = true;
        this.stopReason = stopReason;
    }

    /**
     * 是否正在运行或有队列
     */
    public boolean isRunningOrHasQueue() {
        return running || !triggerQueue.isEmpty();
    }

    @Override
    public void run() {

        // 调用init方法，仅一次
        try {
            handler.init();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        // 执行任务，监听调度中心
        while (!toStop) {
            running = false;
            idleTimes++;

            TriggerRequest triggerParam = null;
            try {
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam != null) {
                    running = true;
                    idleTimes = 0;
                    triggerLogIdSet.remove(triggerParam.getLogId());

                    // 日志文件名，格式 "logPath/yyyy-MM-dd/9999.log"
                    String logFileName = XxlJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());
                    XxlJobContext xxlJobContext = new XxlJobContext(
                            triggerParam.getJobId(),
                            triggerParam.getExecutorParams(),
                            triggerParam.getLogId(),
                            triggerParam.getLogDateTime(),
                            logFileName,
                            triggerParam.getBroadcastIndex(),
                            triggerParam.getBroadcastTotal());

                    // 初始化上下文
                    XxlJobContext.setXxlJobContext(xxlJobContext);

                    // 执行
                    XxlJobHelper.log("<br>----------- xxl-job job execute start -----------<br>----------- Param:" + xxlJobContext.getJobParam());

                    if (triggerParam.getExecutorTimeout() > 0) {
                        // 限制超时
                        Thread futureThread = null;
                        try {
                            java.util.concurrent.FutureTask<Boolean> futureTask = new java.util.concurrent.FutureTask<>(() -> {
                                XxlJobContext.setXxlJobContext(xxlJobContext);
                                handler.execute();
                                return true;
                            });
                            futureThread = new Thread(futureTask);
                            futureThread.setName("xxl-job, JobThread-future-" + jobId + "-" + System.currentTimeMillis());
                            futureThread.start();

                            futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
                        } catch (java.util.concurrent.TimeoutException e) {
                            XxlJobHelper.log("<br>----------- xxl-job job execute timeout");
                            XxlJobHelper.log(e);
                            XxlJobHelper.handleTimeout("job execute timeout ");
                        } finally {
                            if (futureThread != null) {
                                futureThread.interrupt();
                            }
                        }
                    } else {
                        handler.execute();
                    }

                    // 校验执行结果
                    if (XxlJobContext.getXxlJobContext().getHandleCode() <= 0) {
                        XxlJobHelper.handleFail("job handle result lost.");
                    } else {
                        String tempHandleMsg = XxlJobContext.getXxlJobContext().getHandleMsg();
                        tempHandleMsg = (tempHandleMsg != null && tempHandleMsg.length() > 50000)
                                ? tempHandleMsg.substring(0, 50000).concat("...")
                                : tempHandleMsg;
                        XxlJobContext.getXxlJobContext().setHandleMsg(tempHandleMsg);
                    }
                    XxlJobHelper.log("<br>----------- xxl-job job execute end(finish) -----------<br>----------- Result: handleCode="
                            + XxlJobContext.getXxlJobContext().getHandleCode()
                            + ", handleMsg = "
                            + XxlJobContext.getXxlJobContext().getHandleMsg()
                    );

                } else {
                    if (idleTimes > 30) {
                        if (triggerQueue.isEmpty()) {
                            XxlJobExecutor.getInstance().removeJobThread(jobId, "excutor idle times over limit.");
                        }
                    }
                }
            } catch (Throwable e) {
                if (toStop) {
                    XxlJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
                }

                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                String errorMsg = stringWriter.toString();

                XxlJobHelper.handleFail(errorMsg);

                XxlJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- xxl-job job execute end(error) -----------");
            } finally {
                if (triggerParam != null) {
                    // 回调
                    if (!toStop) {
                        XxlJobExecutor.getInstance().getTriggerCallbackHelper().pushCallBack(new CallbackData(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                XxlJobContext.getXxlJobContext().getHandleCode(),
                                XxlJobContext.getXxlJobContext().getHandleMsg()
                        ));
                    } else {
                        XxlJobExecutor.getInstance().getTriggerCallbackHelper().pushCallBack(new CallbackData(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                XxlJobContext.HANDLE_CODE_FAIL,
                                stopReason + " [job running, killed]"
                        ));
                    }
                }
            }
        }

        // 回调队列中未执行的触发请求
        while (triggerQueue != null && !triggerQueue.isEmpty()) {
            TriggerRequest triggerParam = triggerQueue.poll();
            if (triggerParam != null) {
                XxlJobExecutor.getInstance().getTriggerCallbackHelper().pushCallBack(new CallbackData(
                        triggerParam.getLogId(),
                        triggerParam.getLogDateTime(),
                        XxlJobContext.HANDLE_CODE_FAIL,
                        stopReason + " [job not executed, in the job queue, killed.]")
                );
            }
        }

        // 调用destroy方法，仅一次
        try {
            handler.destroy();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        logger.info(">>>>>>>>>>> xxl-job JobThread stoped, hashCode:{}", Thread.currentThread());
    }
}
