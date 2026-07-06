package org.quyq.gwsu.kit.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.job.domain.KitJobLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务日志服务接口
 */
public interface IKitJobLogService extends IService<KitJobLog> {

    /**
     * 查询日志报表统计（Java 中聚合计算，消除 IFNULL/COALESCE 方言差异）
     *
     * @param from 开始时间
     * @param to   结束时间
     * @return 统计结果 {triggerDayCount, triggerDayCountRunning, triggerDayCountSuc}
     */
    Map<String, Object> findLogReport(LocalDateTime from, LocalDateTime to);

    /**
     * 查询待清理的日志ID（Java 中实现分页+子查询逻辑，消除 LIMIT 偏移方言差异）
     *
     * @param jobId          任务ID（可为null）
     * @param clearBeforeTime 清理此时间之前的日志
     * @param clearBeforeNum  保留最近N条
     * @param pagesize       每页数量
     * @return 待清理的日志ID列表
     */
    List<String> findClearLogIds(String jobId, LocalDateTime clearBeforeTime, int clearBeforeNum, int pagesize);

    /**
     * 查询失败告警的日志ID（消除 databaseId 冗余分写）
     *
     * @param pagesize 每页数量
     * @return 失败日志ID列表
     */
    List<String> findFailJobLogIds(int pagesize);

    /**
     * 更新触发信息（标准SQL保留在XML中）
     */
    int updateTriggerInfo(KitJobLog kitJobLog);

    /**
     * 更新处理信息（标准SQL保留在XML中）
     */
    int updateHandleInfo(KitJobLog kitJobLog);

    /**
     * 批量清理日志（标准SQL保留在XML中）
     */
    int clearLog(List<String> logIds);

    /**
     * 更新告警状态（标准SQL保留在XML中）
     */
    int updateAlarmStatus(String logId, int oldAlarmStatus, int newAlarmStatus);

    /**
     * 查询丢失的任务日志ID（LEFT JOIN，标准SQL保留在XML中）
     */
    List<String> findLostJobIds(LocalDateTime losedTime);

}
