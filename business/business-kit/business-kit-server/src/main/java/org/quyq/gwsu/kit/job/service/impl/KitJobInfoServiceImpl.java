package org.quyq.gwsu.kit.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.job.domain.KitJobInfo;
import org.quyq.gwsu.kit.job.mapper.KitJobInfoMapper;
import org.quyq.gwsu.kit.job.service.IKitJobInfoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务信息服务实现
 */
@Service
@RequiredArgsConstructor
public class KitJobInfoServiceImpl extends ServiceImpl<KitJobInfoMapper, KitJobInfo> implements IKitJobInfoService {

    @Override
    public List<KitJobInfo> scheduleJobQuery(long maxNextTime, int pagesize) {
        LambdaQueryWrapper<KitJobInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KitJobInfo::getTriggerStatus, 1)
                .le(KitJobInfo::getTriggerNextTime, maxNextTime)
                .orderByAsc(KitJobInfo::getId);

        Page<KitJobInfo> page = this.page(Page.of(1, pagesize), wrapper);
        return page.getRecords();
    }

    @Override
    public int scheduleBatchUpdate(List<KitJobInfo> jobInfoList) {
        return baseMapper.scheduleBatchUpdate(jobInfoList);
    }

}
