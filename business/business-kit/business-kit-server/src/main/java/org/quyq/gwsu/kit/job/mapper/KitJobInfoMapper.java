package org.quyq.gwsu.kit.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;

import java.util.List;

/**
 * 任务信息Mapper
 */
@Mapper
public interface KitJobInfoMapper {

    List<KitJobInfo> pageList(@Param("offset") int offset,
                              @Param("pagesize") int pagesize,
                              @Param("jobGroup") int jobGroup,
                              @Param("triggerStatus") int triggerStatus,
                              @Param("name") String name,
                              @Param("executorHandler") String executorHandler,
                              @Param("author") String author);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("jobGroup") int jobGroup,
                      @Param("triggerStatus") int triggerStatus,
                      @Param("name") String name,
                      @Param("executorHandler") String executorHandler,
                      @Param("author") String author);

    int save(KitJobInfo info);

    KitJobInfo loadById(@Param("id") int id);

    int update(KitJobInfo kitJobInfo);

    int delete(@Param("id") long id);

    List<KitJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

    int findAllCount();

    /**
     * 查询待调度的任务（trigger_status = 1）
     */
    List<KitJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize);

    /**
     * 更新调度信息
     */
    int scheduleUpdate(KitJobInfo kitJobInfo);

    /**
     * 批量更新调度信息
     */
    int scheduleBatchUpdate(@Param("list") List<KitJobInfo> jobInfoList);

}
