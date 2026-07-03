package org.quyq.gwsu.kit.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.job.dto.KitJobGroupDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobInfoDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobLogDTO;
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
     */
    R<IPage<KitJobInfo>> pageList(KitJobInfoDTO dto);

    /**
     * 添加任务
     */
    R<String> add(KitJobInfo jobInfo);

    /**
     * 更新任务
     */
    R<String> update(KitJobInfo jobInfo);

    /**
     * 删除任务
     */
    R<String> remove(int id);

    /**
     * 启动任务
     */
    R<String> start(int id);

    /**
     * 停止任务
     */
    R<String> stop(int id);

    /**
     * 手动触发一次任务
     */
    R<String> trigger(int jobId, String executorParam, String addressList);

    /**
     * 预估下次触发时间（5次）
     */
    R<List<String>> nextTriggerTime(String scheduleType, String scheduleConf);

    // ==================== 执行器管理 ====================

    /**
     * 分页查询执行器列表
     */
    R<IPage<KitJobGroup>> groupPageList(KitJobGroupDTO dto);

    /**
     * 添加执行器
     */
    R<String> groupAdd(KitJobGroup kitJobGroup);

    /**
     * 更新执行器
     */
    R<String> groupUpdate(KitJobGroup kitJobGroup);

    /**
     * 删除执行器
     */
    R<String> groupRemove(int id);

    /**
     * 根据ID查询执行器
     */
    R<KitJobGroup> groupLoadById(int id);

    // ==================== 日志管理 ====================

    /**
     * 分页查询日志列表
     */
    R<IPage<KitJobLog>> logPageList(KitJobLogDTO dto);

    /**
     * 查询日志详情
     */
    R<KitJobLog> logLoad(long id);

    /**
     * 清理日志
     */
    R<String> logClear(int jobGroup, int jobId, int type);

    // ==================== 仪表盘 ====================

    /**
     * 仪表盘概览信息
     */
    R<Map<String, Object>> dashboardInfo();

    /**
     * 仪表盘图表数据
     */
    R<Map<String, Object>> chartInfo(Date startDate, Date endDate);

}
