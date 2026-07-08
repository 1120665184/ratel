package org.quyq.gwsu.kit.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.quyq.gwsu.kit.job.domain.KitJobLogReport;

/**
 * 任务日志报表Mapper
 */
@Mapper
public interface KitJobLogReportMapper extends BaseMapper<KitJobLogReport> {

    /**
     * 查询日志报表汇总（SUM 聚合）
     */
    KitJobLogReport queryLogReportTotal();

}
