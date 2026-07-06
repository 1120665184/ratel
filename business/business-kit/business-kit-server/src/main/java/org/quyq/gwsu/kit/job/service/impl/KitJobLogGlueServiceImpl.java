package org.quyq.gwsu.kit.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.job.domain.KitJobLogGlue;
import org.quyq.gwsu.kit.job.mapper.KitJobLogGlueMapper;
import org.quyq.gwsu.kit.job.service.IKitJobLogGlueService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务日志Glue服务实现
 */
@Service
@RequiredArgsConstructor
public class KitJobLogGlueServiceImpl extends ServiceImpl<KitJobLogGlueMapper, KitJobLogGlue> implements IKitJobLogGlueService {

    @Override
    public void removeOld(String jobId, int limit) {
        // 1. 查出该 jobId 最近的 limit 条记录 ID（保留列表）
        LambdaQueryWrapper<KitJobLogGlue> retainWrapper = new LambdaQueryWrapper<>();
        retainWrapper.select(KitJobLogGlue::getId)
                .eq(KitJobLogGlue::getJobId, jobId)
                .orderByDesc(KitJobLogGlue::getModifyTime)
                .last("LIMIT " + limit);

        List<KitJobLogGlue> retainList = this.list(retainWrapper);
        Set<String> retainIds = retainList.stream().map(KitJobLogGlue::getId).collect(Collectors.toSet());

        // 2. 查出该 jobId 所有记录 ID
        LambdaQueryWrapper<KitJobLogGlue> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.select(KitJobLogGlue::getId)
                .eq(KitJobLogGlue::getJobId, jobId);

        List<KitJobLogGlue> allList = this.list(allWrapper);

        // 3. 删除不在保留列表中的记录
        List<String> deleteIds = allList.stream()
                .map(KitJobLogGlue::getId)
                .filter(id -> !retainIds.contains(id))
                .toList();

        if (!deleteIds.isEmpty()) {
            this.removeBatchByIds(deleteIds);
        }
    }

}
