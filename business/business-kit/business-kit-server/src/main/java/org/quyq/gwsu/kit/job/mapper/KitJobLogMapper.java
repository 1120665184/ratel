package org.quyq.gwsu.kit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobLog;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务日志Mapper
 */
@Mapper
public interface KitJobLogMapper extends BaseMapper<KitJobLog> {

    /**
     * 更新触发信息
     */
    int updateTriggerInfo(KitJobLog kitJobLog);

    /**
     * 更新处理信息
     */
    int updateHandleInfo(KitJobLog kitJobLog);

    /**
     * 查询日志报表统计（聚合查询，含 IFNULL/COALESCE）
     */
    Map<String, Object> findLogReport(@Param("from") Date from, @Param("to") Date to);

    /**
     * 查询待清理的日志ID
     */
    List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
                               @Param("jobId") int jobId,
                               @Param("clearBeforeTime") Date clearBeforeTime,
                               @Param("clearBeforeNum") int clearBeforeNum,
                               @Param("pagesize") int pagesize);

    /**
     * 批量清理日志
     */
    int clearLog(@Param("logIds") List<Long> logIds);

    /**
     * 查询失败告警的日志ID
     */
    List<Long> findFailJobLogIds(@Param("pagesize") int pagesize);

    /**
     * 更新告警状态
     */
    int updateAlarmStatus(@Param("logId") long logId,
                          @Param("oldAlarmStatus") int oldAlarmStatus,
                          @Param("newAlarmStatus") int newAlarmStatus);

    /**
     * 查询丢失的任务日志ID（LEFT JOIN 子查询）
     */
    List<Long> findLostJobIds(@Param("losedTime") Date losedTime);

}
