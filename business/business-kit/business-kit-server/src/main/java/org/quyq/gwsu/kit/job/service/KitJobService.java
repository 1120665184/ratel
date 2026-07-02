package org.quyq.gwsu.kit.job.service;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 定时任务管理服务
 */
public interface KitJobService {

    // ==================== 任务管理 ====================

    /**
     * 分页查询任务列表
     *
     * @param offset          偏移量
     * @param pagesize        每页大小
     * @param jobGroup        执行器ID
     * @param triggerStatus   调度状态
     * @param name            任务名称
     * @param executorHandler 任务处理器
     * @param author          负责人
     * @return 分页结果
     */
    R<Map<String, Object>> pageList(int offset, int pagesize, int jobGroup, int triggerStatus, String name, String executorHandler, String author);

    /**
     * 添加任务
     *
     * @param jobInfo 任务信息
     * @return 操作结果
     */
    R<String> add(KitJobInfo jobInfo);

    /**
     * 更新任务
     *
     * @param jobInfo 任务信息
     * @return 操作结果
     */
    R<String> update(KitJobInfo jobInfo);

    /**
     * 删除任务
     *
     * @param id 任务ID
     * @return 操作结果
     */
    R<String> remove(int id);

    /**
     * 启动任务
     *
     * @param id 任务ID
     * @return 操作结果
     */
    R<String> start(int id);

    /**
     * 停止任务
     *
     * @param id 任务ID
     * @return 操作结果
     */
    R<String> stop(int id);

    /**
     * 手动触发一次任务
     *
     * @param jobId         任务ID
     * @param executorParam 执行参数
     * @param addressList   执行器地址列表
     * @return 操作结果
     */
    R<String> trigger(int jobId, String executorParam, String addressList);

    /**
     * 预估下次触发时间（5次）
     *
     * @param scheduleType 调度类型
     * @param scheduleConf 调度配置
     * @return 触发时间列表
     */
    R<List<String>> nextTriggerTime(String scheduleType, String scheduleConf);

    // ==================== 执行器管理 ====================

    /**
     * 分页查询执行器列表
     *
     * @param offset   偏移量
     * @param pagesize 每页大小
     * @param appname  应用名称
     * @param name     执行器名称
     * @return 分页结果
     */
    R<Map<String, Object>> groupPageList(int offset, int pagesize, String appname, String name);

    /**
     * 添加执行器
     *
     * @param kitJobGroup 执行器信息
     * @return 操作结果
     */
    R<String> groupAdd(KitJobGroup kitJobGroup);

    /**
     * 更新执行器
     *
     * @param kitJobGroup 执行器信息
     * @return 操作结果
     */
    R<String> groupUpdate(KitJobGroup kitJobGroup);

    /**
     * 删除执行器
     *
     * @param id 执行器ID
     * @return 操作结果
     */
    R<String> groupRemove(int id);

    /**
     * 根据ID查询执行器
     *
     * @param id 执行器ID
     * @return 执行器信息
     */
    R<KitJobGroup> groupLoadById(int id);

    // ==================== 日志管理 ====================

    /**
     * 分页查询日志列表
     *
     * @param offset          偏移量
     * @param pagesize        每页大小
     * @param jobGroup        执行器ID
     * @param jobId           任务ID
     * @param logStatus       日志状态
     * @param triggerTimeStart 触发时间起始
     * @param triggerTimeEnd   触发时间截止
     * @return 分页结果
     */
    R<Map<String, Object>> logPageList(int offset, int pagesize, int jobGroup, int jobId, int logStatus, Date triggerTimeStart, Date triggerTimeEnd);

    /**
     * 查询日志详情
     *
     * @param id 日志ID
     * @return 日志信息
     */
    R<KitJobLog> logLoad(long id);

    /**
     * 清理日志
     *
     * @param jobGroup 执行器ID
     * @param jobId    任务ID
     * @param type     清理类型
     * @return 操作结果
     */
    R<String> logClear(int jobGroup, int jobId, int type);

    // ==================== 仪表盘 ====================

    /**
     * 仪表盘概览信息
     *
     * @return 概览数据
     */
    R<Map<String, Object>> dashboardInfo();

    /**
     * 仪表盘图表数据
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 图表数据
     */
    R<Map<String, Object>> chartInfo(Date startDate, Date endDate);

}
