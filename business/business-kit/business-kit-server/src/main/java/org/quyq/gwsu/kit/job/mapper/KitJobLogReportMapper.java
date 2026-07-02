package org.quyq.gwsu.kit.job.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.quyq.gwsu.kit.job.domain.KitJobLogReport;

import java.util.Date;
import java.util.List;

/**
 * 任务日志报表Mapper
 */
@Mapper
public interface KitJobLogReportMapper {

    int saveOrUpdate(KitJobLogReport kitJobLogReport);

    List<KitJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
                                          @Param("triggerDayTo") Date triggerDayTo);

    KitJobLogReport queryLogReportTotal();

}
