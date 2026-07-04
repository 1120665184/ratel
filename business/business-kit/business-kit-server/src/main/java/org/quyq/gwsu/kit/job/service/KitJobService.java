package org.quyq.gwsu.kit.job.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.job.dto.KitJobGroupDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobInfoDTO;
import org.quyq.gwsu.kit.api.job.dto.KitJobLogDTO;
import org.quyq.gwsu.kit.job.domain.KitJobGroup;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.domain.KitJobLog;

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

    // ==================== 执行器管理 ====================

    R<IPage<KitJobGroup>> groupPageList(KitJobGroupDTO dto);

    R<String> groupAdd(KitJobGroup kitJobGroup);

    R<String> groupUpdate(KitJobGroup kitJobGroup);

    R<String> groupRemove(String id);

    R<KitJobGroup> groupLoadById(String id);

    // ==================== 日志管理 ====================

    R<IPage<KitJobLog>> logPageList(KitJobLogDTO dto);

    R<KitJobLog> logLoad(String id);

    R<String> logClear(String jobGroup, String jobId, int type);

    // ==================== 仪表盘 ====================

    R<Map<String, Object>> dashboardInfo();

    R<Map<String, Object>> chartInfo(LocalDateTime startDate, LocalDateTime endDate);

}
