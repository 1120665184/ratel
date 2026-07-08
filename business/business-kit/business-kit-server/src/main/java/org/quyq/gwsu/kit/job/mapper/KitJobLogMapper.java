package org.quyq.gwsu.kit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobLog;

import java.time.LocalDateTime;
import java.util.List;

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
     * 批量清理日志
     */
    int clearLog(@Param("logIds") List<String> logIds);

    /**
     * 更新告警状态
     */
    int updateAlarmStatus(@Param("logId") String logId,
                          @Param("oldAlarmStatus") int oldAlarmStatus,
                          @Param("newAlarmStatus") int newAlarmStatus);

    /**
     * 查询丢失的任务日志ID（LEFT JOIN 子查询）
     */
    List<String> findLostJobIds(@Param("losedTime") LocalDateTime losedTime);

}
