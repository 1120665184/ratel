package org.quyq.gwsu.kit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobLogGlue;

/**
 * 任务日志Glue Mapper
 */
@Mapper
public interface KitJobLogGlueMapper extends BaseMapper<KitJobLogGlue> {

    /**
     * 删除旧的Glue记录（保留最近limit条）
     */
    int removeOld(@Param("jobId") String jobId, @Param("limit") int limit);

}
