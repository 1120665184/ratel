package org.quyq.gwsu.kit.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.quyq.gwsu.kit.api.job.dto.JobInfoCreateDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobInfoDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobLogDTO;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;
import org.quyq.gwsu.kit.job.domain.KitJobLogGlue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定时任务管理服务
 */
public interface KitJobService {

    // ==================== 任务管理 ====================

    R<IPage<KitJobInfo>> pageList(KitJobInfoDTO dto);

    R<String> add(KitJobInfo jobInfo);

    R<String> update(KitJobInfo jobInfo);

    R<String> remove(String id);

    R<String> start(String id);

    R<String> stop(String id);

    R<String> trigger(String jobId, String executorParam, String addressList);

    R<List<String>> nextTriggerTime(String scheduleType, String scheduleConf);

    // ==================== 调度界面适配 ====================

    /**
     * DTO适配新增任务
     */
    R<String> addByDTO(JobInfoCreateDTO dto);

    /**
     * DTO适配更新任务
     */
    R<String> updateByDTO(JobInfoCreateDTO dto);

    /**
     * 终止运行中的任务
     */
    R<String> kill(String logId);

    /**
     * 读取执行器端完整日志
     */
    R<LogData> logContent(String logId, int fromLineNum);

    /**
     * 查询所有在线Handler名称（过滤urlJobHandler）
     */
    R<List<String>> handlerList();

    /**
     * 查询GLUE版本历史
     */
    R<List<KitJobLogGlue>> glueVersionList(String jobId);

    /**
     * 查询GLUE版本详情
     */
    R<KitJobLogGlue> glueVersionDetail(String id);

    // ==================== 日志管理 ====================

    R<IPage<KitJobLog>> logPageList(KitJobLogDTO dto);

    R<KitJobLog> logLoad(String id);

    R<String> logClear(String jobId, int type);

    // ==================== 仪表盘 ====================

    R<Map<String, Object>> dashboardInfo();

    R<Map<String, Object>> chartInfo(LocalDateTime startDate, LocalDateTime endDate);

}
