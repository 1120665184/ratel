package org.quyq.gwsu.security.dataresource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.dataresource.DataResourceRuleUtils;
import org.quyq.gwsu.common.security.domain.DataResoureRule;
import org.quyq.gwsu.common.security.enums.DataResourceAssertType;
import org.quyq.gwsu.common.security.enums.DataResourceFieldConditionType;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceConditionSaveDTO;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceQueryDTO;
import org.quyq.gwsu.security.api.dataresource.dto.DataResourceSaveDTO;
import org.quyq.gwsu.security.api.dataresource.vo.DataResourceVO;
import org.quyq.gwsu.security.dataresource.domain.SecurityDataResource;
import org.quyq.gwsu.security.dataresource.domain.SecurityDataResourceCondition;
import org.quyq.gwsu.security.dataresource.mapper.SecurityDataResourceConditionMapper;
import org.quyq.gwsu.security.dataresource.mapper.SecurityDataResourceMapper;
import org.quyq.gwsu.security.dataresource.service.ISecurityDataResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据资源配置服务实现
 *
 * @author Quyq
 * @date 2026/4/20
 */
@Service
@RequiredArgsConstructor
public class SecurityDataResourceServiceImpl extends ServiceImpl<SecurityDataResourceMapper, SecurityDataResource>
        implements ISecurityDataResourceService {

    private final SecurityDataResourceConditionMapper conditionMapper;

    private final CacheUtils cacheUtils;

    @Override
    public DataResourceVO getById(Long id) {
        SecurityDataResource entity = super.getById(id);
        if (entity == null) {
            return null;
        }
        DataResourceVO vo = entity.toVo();

        // 查询条件列表
        List<SecurityDataResourceCondition> conditions = conditionMapper.selectList(
                new LambdaQueryWrapper<SecurityDataResourceCondition>()
                        .eq(SecurityDataResourceCondition::getDataResourceId, id)
                        .orderByAsc(SecurityDataResourceCondition::getSort)
        );
        vo.setConditions(conditions.stream().map(SecurityDataResourceCondition::toVo).toList());

        return vo;
    }

    @Override
    public List<DataResourceVO> listByTableName(String tableName) {
        List<SecurityDataResource> list = list(
                new LambdaQueryWrapper<SecurityDataResource>()
                        .eq(SecurityDataResource::getTableName, tableName)
                        .eq(SecurityDataResource::getStatus, true)
        );
        return list.stream().map(SecurityDataResource::toVo).toList();
    }

    @Override
    public IPage<DataResourceVO> pageByCondition(DataResourceQueryDTO query) {
        Page<SecurityDataResource> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<SecurityDataResource> wrapper = new LambdaQueryWrapper<SecurityDataResource>()
                .like(query.getTableName() != null, SecurityDataResource::getTableName, query.getTableName())
                .like(query.getDatabaseName() != null, SecurityDataResource::getDatabaseName, query.getDatabaseName())
                .eq(query.getStatus() != null, SecurityDataResource::getStatus, query.getStatus())
                .orderByDesc(SecurityDataResource::getCreateTime);

        IPage<SecurityDataResource> entityPage = page(page, wrapper);

        return entityPage.convert(entity -> {
            DataResourceVO vo = entity.toVo();
            // 查询条件列表
            List<SecurityDataResourceCondition> conditions = conditionMapper.selectList(
                    new LambdaQueryWrapper<SecurityDataResourceCondition>()
                            .eq(SecurityDataResourceCondition::getDataResourceId, entity.getId())
                            .orderByAsc(SecurityDataResourceCondition::getSort)
            );
            vo.setConditions(conditions.stream().map(SecurityDataResourceCondition::toVo).toList());
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveOrUpdate(DataResourceSaveDTO dto) {
        SecurityDataResource entity = new SecurityDataResource();
        entity.setId(dto.getId());
        entity.setDatabaseName(dto.getDatabaseName());
        entity.setTableName(dto.getTableName());
        entity.setDescription(dto.getDescription());
        entity.setSupportSelfOnly(dto.getSupportSelfOnly());
        entity.setSelfOnlyField(dto.getSelfOnlyField());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : Boolean.TRUE);

        // 保存或更新主表
        saveOrUpdate(entity);

        // 删除旧条件
        if (dto.getId() != null) {
            conditionMapper.delete(
                    new LambdaQueryWrapper<SecurityDataResourceCondition>()
                            .eq(SecurityDataResourceCondition::getDataResourceId, dto.getId())
            );
        }

        // 保存新条件
        if (!CollectionUtils.isEmpty(dto.getConditions())) {
            List<SecurityDataResourceCondition> conditions = dto.getConditions().stream()
                    .map(this::convertToEntity)
                    .peek(c -> c.setDataResourceId(entity.getId()))
                    .toList();
            conditionMapper.insert(conditions);
        }

        // 同步到 Redis
        syncToRedis();

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        if (CollectionUtils.isEmpty(list)) {
            return false;
        }

        // 删除条件
        conditionMapper.delete(
                new LambdaQueryWrapper<SecurityDataResourceCondition>()
                        .in(SecurityDataResourceCondition::getDataResourceId, list)
        );

        // 删除主表
        super.removeByIds(list);

        // 同步到 Redis
        syncToRedis();

        return true;
    }

    @Override
    public List<DataResoureRule> getAllEnabledRules() {
        // 查询所有启用的数据资源配置
        List<SecurityDataResource> dataResources = this.list(
                new LambdaQueryWrapper<SecurityDataResource>()
                        .eq(SecurityDataResource::getStatus, true)
        );

        if (CollectionUtils.isEmpty(dataResources)) {
            return Collections.emptyList();
        }

        // 查询所有条件
        List<String> dataResourceIds = dataResources.stream()
                .map(SecurityDataResource::getId)
                .toList();

        List<SecurityDataResourceCondition> allConditions = conditionMapper.selectList(
                new LambdaQueryWrapper<SecurityDataResourceCondition>()
                        .in(SecurityDataResourceCondition::getDataResourceId, dataResourceIds)
                        .orderByAsc(SecurityDataResourceCondition::getSort)
        );

        // 按数据资源ID分组
        var conditionMap = allConditions.stream()
                .collect(Collectors.groupingBy(SecurityDataResourceCondition::getDataResourceId));

        // 转换为 DataResoureRule
        return dataResources.stream()
                .map(dr -> {
                    DataResoureRule rule = new DataResoureRule();
                    rule.setDatabaseName(dr.getDatabaseName());
                    rule.setTableName(dr.getTableName());
                    rule.setSupportSelfOnly(dr.getSupportSelfOnly());
                    rule.setSelfOnlyField(dr.getSelfOnlyField());

                    List<SecurityDataResourceCondition> conditions = conditionMap.getOrDefault(dr.getId(), Collections.emptyList());
                    List<DataResoureRule.FieldCondition> fieldConditions = conditions.stream()
                            .map(this::convertToFieldCondition)
                            .toList();
                    rule.setConditions(fieldConditions);

                    return rule;
                })
                .toList();
    }

    @Override
    public Boolean syncToRedis() {
        List<DataResoureRule> rules = getAllEnabledRules();
        cacheUtils.withRebel(() -> {
            cacheUtils.set(SecurityConstants.DataResource.DATA_RESOURCE_RULES_CACHE_KEY, new DataResourceRuleUtils.DataResourceRuleList(rules));
            return null;
        });
        return true;
    }

    /**
     * 转换 DTO 为实体
     */
    private SecurityDataResourceCondition convertToEntity(DataResourceConditionSaveDTO dto) {
        SecurityDataResourceCondition entity = new SecurityDataResourceCondition();
        entity.setId(dto.getId());
        entity.setFieldName(dto.getFieldName());
        entity.setShowNull(dto.getShowNull());
        entity.setSort(dto.getSort());

        // 处理用户资源字段
        if (!CollectionUtils.isEmpty(dto.getUserResourceFields())) {
            entity.setUserResourceFields(String.join(",", dto.getUserResourceFields()));
        }

        // 处理断言类型
        if (dto.getAssertType() != null) {
            entity.setAssertType(DataResourceAssertType.valueOf(dto.getAssertType()));
        }

        // 处理关联关系
        if (dto.getRelationship() != null) {
            entity.setRelationship(DataResourceFieldConditionType.valueOf(dto.getRelationship()));
        }

        return entity;
    }

    /**
     * 转换为 FieldCondition
     */
    private DataResoureRule.FieldCondition convertToFieldCondition(SecurityDataResourceCondition condition) {
        DataResoureRule.FieldCondition fc = new DataResoureRule.FieldCondition();
        fc.setFieldName(condition.getFieldName());
        fc.setShowNull(Boolean.TRUE.equals(condition.getShowNull()));
        fc.setAssertType(condition.getAssertType());
        fc.setRelationship(condition.getRelationship() != null ? condition.getRelationship() : DataResourceFieldConditionType.AND);
        fc.setUserResourceFields(condition.parseUserResourceFields());
        return fc;
    }

}
