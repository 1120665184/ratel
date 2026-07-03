package org.quyq.gwsu.kit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;

import java.util.List;

/**
 * 任务信息Mapper
 */
@Mapper
public interface KitJobInfoMapper extends BaseMapper<KitJobInfo> {

    /**
     * 查询待调度的任务（trigger_status = 1 且 trigger_next_time <= maxNextTime）
     */
    List<KitJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize);

    /**
     * 更新调度信息（仅 trigger_status = 1 的记录）
     */
    int scheduleUpdate(KitJobInfo kitJobInfo);

    /**
     * 批量更新调度信息（CASE WHEN）
     */
    int scheduleBatchUpdate(@Param("list") List<KitJobInfo> jobInfoList);

}
