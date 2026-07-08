package org.quyq.gwsu.kit.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.job.domain.KitJobRegistry;
import org.quyq.gwsu.kit.job.mapper.KitJobRegistryMapper;
import org.quyq.gwsu.kit.job.service.IKitJobRegistryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行器注册服务实现
 */
@Service
@RequiredArgsConstructor
public class KitJobRegistryServiceImpl extends ServiceImpl<KitJobRegistryMapper, KitJobRegistry> implements IKitJobRegistryService {

    @Override
    public List<String> findDead(int timeout, LocalDateTime nowTime) {
        // Java 中计算截止时间，消除 DATE_ADD/INTERVAL 方言差异
        LocalDateTime deadline = nowTime.minusSeconds(timeout);

        LambdaQueryWrapper<KitJobRegistry> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(KitJobRegistry::getId)
                .lt(KitJobRegistry::getModifyTime, deadline);

        List<KitJobRegistry> list = this.list(wrapper);
        return list.stream().map(KitJobRegistry::getId).toList();
    }

    @Override
    public List<KitJobRegistry> findAll(int timeout, LocalDateTime nowTime) {
        // Java 中计算截止时间
        LocalDateTime deadline = nowTime.minusSeconds(timeout);

        LambdaQueryWrapper<KitJobRegistry> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(KitJobRegistry::getModifyTime, deadline);

        return this.list(wrapper);
    }

    @Override
    public void registrySaveOrUpdate(String id, String registryGroup, String registryKey, String registryValue, LocalDateTime modifyTime) {
        // 先查是否存在，消除 ON DUPLICATE KEY / ON CONFLICT 方言差异
        LambdaQueryWrapper<KitJobRegistry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KitJobRegistry::getRegistryGroup, registryGroup)
                .eq(KitJobRegistry::getRegistryKey, registryKey)
                .eq(KitJobRegistry::getRegistryValue, registryValue);

        KitJobRegistry existing = this.getOne(wrapper);

        if (existing != null) {
            // 更新
            existing.setModifyTime(modifyTime);
            this.updateById(existing);
        } else {
            // 新增
            KitJobRegistry registry = new KitJobRegistry();
            registry.setId(id);
            registry.setRegistryGroup(registryGroup);
            registry.setRegistryKey(registryKey);
            registry.setRegistryValue(registryValue);
            registry.setModifyTime(modifyTime);
            this.save(registry);
        }
    }

    @Override
    public String findConflictAppname(String registryGroup, String registryKey) {
        LambdaQueryWrapper<KitJobRegistry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KitJobRegistry::getRegistryKey, registryKey)
                .ne(KitJobRegistry::getRegistryGroup, registryGroup)
                .select(KitJobRegistry::getRegistryGroup)
                .last("LIMIT 1");

        KitJobRegistry conflict = this.getOne(wrapper);
        return conflict != null ? conflict.getRegistryGroup() : null;
    }

}
