package org.quyq.gwsu.kit.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.job.domain.KitJobLogReport;

/**
 * 任务日志报表服务接口
 */
public interface IKitJobLogReportService extends IService<KitJobLogReport> {

    /**
     * 保存或更新报表（先查后决定 insert/update，消除 ON DUPLICATE KEY / ON CONFLICT 方言差异）
     *
     * @param kitJobLogReport 报表数据
     */
    void saveOrUpdateReport(KitJobLogReport kitJobLogReport);

    /**
     * 查询日志报表汇总（SUM聚合，标准SQL保留在XML中）
     */
    KitJobLogReport queryLogReportTotal();

}
