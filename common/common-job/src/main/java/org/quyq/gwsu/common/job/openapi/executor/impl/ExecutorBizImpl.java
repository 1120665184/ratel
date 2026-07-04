package org.quyq.gwsu.common.job.openapi.executor.impl;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.constant.ExecutorBlockStrategyEnum;
import org.quyq.gwsu.common.job.context.XxlJobContext;
import org.quyq.gwsu.common.job.executor.XxlJobExecutor;
import org.quyq.gwsu.common.job.glue.GlueFactory;
import org.quyq.gwsu.common.job.glue.GlueTypeEnum;
import org.quyq.gwsu.common.job.handler.IJobHandler;
import org.quyq.gwsu.common.job.handler.impl.GlueJobHandler;
import org.quyq.gwsu.common.job.handler.impl.ScriptJobHandler;
import org.quyq.gwsu.common.job.log.XxlJobFileAppender;
import org.quyq.gwsu.common.job.openapi.executor.ExecutorBiz;
import org.quyq.gwsu.common.job.openapi.executor.dto.IdleBeatRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.KillRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.common.job.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 执行器业务实现
 */
public class ExecutorBizImpl implements ExecutorBiz {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

    @Override
    public R<String> beat() {
        return R.ok();
    }

    @Override
    public R<String> idleBeat(IdleBeatRequest idleBeatRequest) {

        boolean isRunningOrHasQueue = false;
        JobThread jobThread = XxlJobExecutor.getInstance().loadJobThread(idleBeatRequest.getJobId());
        if (jobThread != null && jobThread.isRunningOrHasQueue()) {
            isRunningOrHasQueue = true;
        }

        if (isRunningOrHasQueue) {
            return R.fail("job thread is running or has trigger queue.");
        }
        return R.ok();
    }

    @Override
    public R<String> trigger(TriggerRequest triggerRequest) {

        // 加载任务信息
        JobThread jobThread = XxlJobExecutor.getInstance().loadJobThread(triggerRequest.getJobId());
        IJobHandler jobHandler = jobThread != null ? jobThread.getHandler() : null;
        String removeOldReason = null;
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerRequest.getGlueType());

        // 校验非BEAN类型的Glue是否启用
        if (glueTypeEnum != null && GlueTypeEnum.BEAN != glueTypeEnum) {
            boolean glueEnabled = XxlJobExecutor.getInstance().getGlueEnabled();
            if (!glueEnabled) {
                logger.warn(">>>>>>>>>>> xxl-job executor not support current glue type[{}], please check executor configuration.", glueTypeEnum.getDesc());
                return new R<>(XxlJobContext.HANDLE_CODE_FAIL, "fail, current glue type [" + glueTypeEnum.getDesc() + "] not supported.", null, null);
            }
        }

        // 分派处理器
        if (GlueTypeEnum.BEAN == glueTypeEnum) {

            IJobHandler newJobHandler = XxlJobExecutor.getInstance().loadJobHandler(triggerRequest.getExecutorHandler());

            if (jobThread != null && jobHandler != newJobHandler) {
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";
                jobThread = null;
                jobHandler = null;
            }

            if (jobHandler == null) {
                jobHandler = newJobHandler;
                if (jobHandler == null) {
                    return new R<>(XxlJobContext.HANDLE_CODE_FAIL, "job handler [" + triggerRequest.getExecutorHandler() + "] not found.", null, null);
                }
            }

        } else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {

            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof GlueJobHandler
                            && ((GlueJobHandler) jobThread.getHandler()).getGlueUpdatetime() == triggerRequest.getGlueUpdatetime())) {
                removeOldReason = "change job source or glue type, and terminate the old job thread.";
                jobThread = null;
                jobHandler = null;
            }

            if (jobHandler == null) {
                try {
                    IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerRequest.getGlueSource());
                    jobHandler = new GlueJobHandler(originJobHandler, triggerRequest.getGlueUpdatetime());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    return new R<>(XxlJobContext.HANDLE_CODE_FAIL, e.getMessage(), null, null);
                }
            }
        } else if (glueTypeEnum != null && glueTypeEnum.isScript()) {

            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof ScriptJobHandler
                            && ((ScriptJobHandler) jobThread.getHandler()).getGlueUpdatetime() == triggerRequest.getGlueUpdatetime())) {
                removeOldReason = "change job source or glue type, and terminate the old job thread.";
                jobThread = null;
                jobHandler = null;
            }

            if (jobHandler == null) {
                jobHandler = new ScriptJobHandler(triggerRequest.getJobId(), triggerRequest.getGlueUpdatetime(), triggerRequest.getGlueSource(), GlueTypeEnum.match(triggerRequest.getGlueType()));
            }
        } else {
            return new R<>(XxlJobContext.HANDLE_CODE_FAIL, "glueType[" + triggerRequest.getGlueType() + "] is not valid.", null, null);
        }

        // 执行器阻塞策略
        if (jobThread != null) {
            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerRequest.getExecutorBlockStrategy(), null);
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
                if (jobThread.isRunningOrHasQueue()) {
                    return new R<>(XxlJobContext.HANDLE_CODE_FAIL, "block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle(), null, null);
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                if (jobThread.isRunningOrHasQueue()) {
                    removeOldReason = "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();
                    jobThread = null;
                }
            }
        }

        // 替换线程（新建或已失效）
        if (jobThread == null) {
            jobThread = XxlJobExecutor.getInstance().registJobThread(triggerRequest.getJobId(), jobHandler, removeOldReason);
        }

        // 推送到触发队列
        return jobThread.pushTriggerQueue(triggerRequest);
    }

    @Override
    public R<String> kill(KillRequest killRequest) {
        JobThread jobThread = XxlJobExecutor.getInstance().loadJobThread(killRequest.getJobId());
        if (jobThread != null) {
            XxlJobExecutor.getInstance().removeJobThread(killRequest.getJobId(), "scheduling center kill job.");
            return R.ok();
        }

        return R.ok("job thread already killed.");
    }

    @Override
    public R<LogData> log(LogRequest logRequest) {
        String logFileName = XxlJobFileAppender.makeLogFileName(LocalDateTime.ofInstant(Instant.ofEpochMilli(logRequest.getLogDateTime()), ZoneId.systemDefault()), logRequest.getLogId());

        LogData logResult = XxlJobFileAppender.readLog(logFileName, logRequest.getFromLineNum());
        return R.ok(logResult);
    }

}
