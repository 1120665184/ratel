package org.quyq.gwsu.security.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.api.vo.ConfigVO;
import org.quyq.gwsu.security.api.config.dto.ConfigQueryDTO;
import org.quyq.gwsu.security.api.config.dto.ConfigSaveDTO;
import org.quyq.gwsu.security.dict.domain.SecurityConfig;
import org.quyq.gwsu.security.dict.mapper.SecurityConfigMapper;
import org.quyq.gwsu.security.dict.service.ISecurityConfigService;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 配置服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = ISecurityConfigService.CACHE_CONFIG_PREFIX)
public class SecurityConfigServiceImpl extends ServiceImpl<SecurityConfigMapper, SecurityConfig> implements ISecurityConfigService {


    @Override
    public ConfigVO getById(String id) {
        SecurityConfig config = super.getById(id);
        return config != null ? config.toVo() : null;
    }

    @Cacheable(key = "#configKey")
    @Override
    public ConfigVO getByKey(String configKey) {
        SecurityConfig config = getOne(new LambdaQueryWrapper<SecurityConfig>()
                .eq(SecurityConfig::getConfigKey, configKey)
                .eq(SecurityConfig::getDeleted, false));
        return config != null ? config.toVo() : null;
    }

    @Override
    public Map<String, ConfigVO> getByKeys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }
        Map<String, ConfigVO> finV = new LinkedHashMap<>(keys.size());
        for (String key : keys) {
            ConfigVO val = SpringUtils.getAopProxy(this).getByKey(key);
            if (Objects.nonNull(val)) {
                finV.put(key, val);
            }
        }
        return finV;
    }

    @Override
    public IPage<ConfigVO> pageByCondition(ConfigQueryDTO query) {
        LambdaQueryWrapper<SecurityConfig> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SecurityConfig::getModifyTime);

        Page<SecurityConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SecurityConfig> configPage = page(page, wrapper);

        return configPage.convert(SecurityConfig::toVo);
    }

    @Override
    @CacheEvict(key = "#dto.configKey")
    public Boolean saveOrUpdateConfig(ConfigSaveDTO dto) {
        SecurityConfig entity = new SecurityConfig();
        entity.setId(dto.getId());
        entity.setConfigKey(dto.getConfigKey());
        entity.setConfigName(dto.getConfigName());
        entity.setConfigValue(dto.getConfigValue());
        entity.setValueType(dto.getValueType());
        entity.setDescription(dto.getDescription());

        if (dto.getId() == null || dto.getId().isEmpty()) {
            // 新增：校验配置键唯一性
            SecurityConfig existing = getOne(new LambdaQueryWrapper<SecurityConfig>()
                    .eq(SecurityConfig::getConfigKey, dto.getConfigKey())
                    .eq(SecurityConfig::getDeleted, false));
            if (existing != null) {
                throw new BusinessException(SecurityErrorCode.E05001);
            }
            entity.setConfigType(2);
        } else {
            // 更新：保留系统字段
            SecurityConfig existing = super.getById(dto.getId());
            if (existing == null) {
                throw new BusinessException(SecurityErrorCode.E05003);
            }
            entity.setConfigType(existing.getConfigType());
            // 系统配置不可修改配置键
            if (existing.getConfigType() == 1 && !existing.getConfigKey().equals(dto.getConfigKey())) {
                entity.setConfigKey(existing.getConfigKey());
            }
        }
        return saveOrUpdate(entity);
    }

    @Override
    @CacheEvict(allEntries = true)
    public Boolean removeByIds(List<String> ids) {
        List<SecurityConfig> configs = listByIds(ids);
        for (SecurityConfig config : configs) {
            if (config.getConfigType() == 1) {
                throw new BusinessException(SecurityErrorCode.E05002);
            }
        }
        return removeBatchByIds(ids);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SecurityConfig> buildQueryWrapper(ConfigQueryDTO query) {
        LambdaQueryWrapper<SecurityConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityConfig::getDeleted, false);

        if (query != null) {
            if (query.getConfigKey() != null && !query.getConfigKey().isEmpty()) {
                wrapper.like(SecurityConfig::getConfigKey, query.getConfigKey());
            }
            if (query.getConfigName() != null && !query.getConfigName().isEmpty()) {
                wrapper.like(SecurityConfig::getConfigName, query.getConfigName());
            }
            if (query.getValueType() != null) {
                wrapper.eq(SecurityConfig::getValueType, query.getValueType());
            }
            if (query.getConfigType() != null) {
                wrapper.eq(SecurityConfig::getConfigType, query.getConfigType());
            }
        }
        return wrapper;
    }
}
