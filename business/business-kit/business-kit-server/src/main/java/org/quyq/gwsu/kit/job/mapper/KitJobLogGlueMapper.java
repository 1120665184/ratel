package org.quyq.gwsu.kit.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobLogGlue;

import java.util.List;

/**
 * 任务日志Glue Mapper
 */
@Mapper
public interface KitJobLogGlueMapper {

    int save(KitJobLogGlue kitJobLogGlue);

    List<KitJobLogGlue> findByJobId(@Param("jobId") int jobId);

    int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

    int deleteByJobId(@Param("jobId") int jobId);

}
