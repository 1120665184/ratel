package org.quyq.gwsu.kit.job.scheduler.trigger;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.IdleBeatRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.KillRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;

/**
 * 触发策略接口（单体/分布式双模式）
 */
public interface TriggerStrategy {

    /**
     * 触发任务
     *
     * @param address        执行器地址
     * @param triggerRequest 触发请求
     * @return 响应
     */
    R<String> trigger(String address, TriggerRequest triggerRequest);

    /**
     * 心跳检测
     *
     * @param address 执行器地址
     * @return 响应
     */
    R<String> beat(String address);

    /**
     * 空闲检测
     *
     * @param address        执行器地址
     * @param idleBeatRequest 空闲检测请求
     * @return 响应
     */
    R<String> idleBeat(String address, IdleBeatRequest idleBeatRequest);

    /**
     * 终止任务
     *
     * @param address     执行器地址
     * @param killRequest 终止请求
     * @return 响应
     */
    R<String> kill(String address, KillRequest killRequest);

    /**
     * 查询日志
     *
     * @param address    执行器地址
     * @param logRequest 日志请求
     * @return 响应
     */
    R<LogData> log(String address, LogRequest logRequest);

}
