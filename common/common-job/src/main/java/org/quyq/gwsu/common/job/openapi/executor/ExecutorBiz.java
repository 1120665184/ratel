package org.quyq.gwsu.common.job.openapi.executor;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.IdleBeatRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.KillRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;

/**
 * 执行器业务接口
 */
public interface ExecutorBiz {

    /**
     * 心跳检测
     *
     * @return 响应
     */
    R<String> beat();

    /**
     * 空闲检测
     *
     * @param idleBeatRequest 空闲检测请求
     * @return 响应
     */
    R<String> idleBeat(IdleBeatRequest idleBeatRequest);

    /**
     * 触发任务
     *
     * @param triggerRequest 触发请求
     * @return 响应
     */
    R<String> trigger(TriggerRequest triggerRequest);

    /**
     * 终止任务
     *
     * @param killRequest 终止请求
     * @return 响应
     */
    R<String> kill(KillRequest killRequest);

    /**
     * 查询日志
     *
     * @param logRequest 日志请求
     * @return 响应
     */
    R<LogData> log(LogRequest logRequest);

}
