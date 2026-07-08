package org.quyq.gwsu.kit.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.job.domain.KitJobLogReport;
import org.quyq.gwsu.kit.job.mapper.KitJobLogReportMapper;
import org.quyq.gwsu.kit.job.service.IKitJobLogReportService;
import org.springframework.stereotype.Service;

/**
 * 任务日志报表服务实现
 */
@Service
@RequiredArgsConstructor
public class KitJobLogReportServiceImpl extends ServiceImpl<KitJobLogReportMapper, KitJobLogReport> implements IKitJobLogReportService {

    @Override
    public void saveOrUpdateReport(KitJobLogReport kitJobLogReport) {
        // 先按 triggerDay 查询是否存在，消除 ON DUPLICATE KEY / ON CONFLICT 方言差异
        LambdaQueryWrapper<KitJobLogReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KitJobLogReport::getTriggerDay, kitJobLogReport.getTriggerDay());

        KitJobLogReport existing = this.getOne(wrapper);

        if (existing != null) {
            // 更新
            existing.setRunningCount(kitJobLogReport.getRunningCount());
            existing.setSucCount(kitJobLogReport.getSucCount());
            existing.setFailCount(kitJobLogReport.getFailCount());
            existing.setModifyTime(kitJobLogReport.getModifyTime());
            this.updateById(existing);
        } else {
            // 新增
            this.save(kitJobLogReport);
        }
    }

    @Override
    public KitJobLogReport queryLogReportTotal() {
        return baseMapper.queryLogReportTotal();
    }

}
