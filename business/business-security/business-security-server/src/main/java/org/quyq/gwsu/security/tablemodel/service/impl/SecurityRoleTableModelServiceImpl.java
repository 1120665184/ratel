package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.tablemodel.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.RoleTableModelVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.tablemodel.domain.SecurityRoleTableModel;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityRoleTableModelMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityRoleTableModelService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityRoleTableModelServiceImpl extends ServiceImpl<SecurityRoleTableModelMapper, SecurityRoleTableModel>
        implements ISecurityRoleTableModelService {

    @Override
    public IPage<RoleTableModelVO> pageByCondition(TableModelQueryDTO query) {
        Page<SecurityRoleTableModel> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SecurityRoleTableModel> wrapper = new LambdaQueryWrapper<>();
        if (query.getRoleId() != null) {
            wrapper.eq(SecurityRoleTableModel::getRoleId, query.getRoleId());
        }
        if (query.getModulePrefix() != null) {
            wrapper.eq(SecurityRoleTableModel::getModulePrefix, query.getModulePrefix());
        }
        if (query.getTableName() != null) {
            wrapper.like(SecurityRoleTableModel::getTableName, query.getTableName());
        }
        return page(page, wrapper).convert(SecurityRoleTableModel::toVo);
    }

    @Override
    public List<RoleTableModelVO> listByRoleId(String roleId) {
        return list(new LambdaQueryWrapper<SecurityRoleTableModel>()
                .eq(SecurityRoleTableModel::getRoleId, roleId))
                .stream()
                .map(SecurityRoleTableModel::toVo)
                .toList();
    }

    @Override
    public Boolean saveOrUpdateRoleTableModel(RoleTableModelSaveDTO dto) {
        AssertUtils.hasText(dto.getRoleId(), SecurityErrorCode.E03003);
        AssertUtils.hasText(dto.getTableName(), SecurityErrorCode.E03001);

        // 构建字段配置
        Map<String, FieldPermission> fieldConfigMap = null;
        if (!CollectionUtils.isEmpty(dto.getFields())) {
            fieldConfigMap = new HashMap<>();
            for (RoleTableModelSaveDTO.FieldConfigItem item : dto.getFields()) {
                fieldConfigMap.put(item.getFieldName(), new FieldPermission(
                        item.getShow() != null ? item.getShow() : true,
                        item.getDesensitize() != null ? item.getDesensitize() : false,
                        item.getStrategy() != null ? SensitiveStrategy.valueOf(item.getStrategy()) : SensitiveStrategy.NONE,
                        item.getPrefixNoMaskLen(),
                        item.getSuffixNoMaskLen(),
                        item.getSymbol()
                ));
            }
        }

        // 查找已有记录（唯一索引：roleId + modulePrefix + datasource + tableName）
        String modulePrefix = dto.getModulePrefix() != null ? dto.getModulePrefix() : "";
        String datasource = dto.getDatasource() != null ? dto.getDatasource() : "master";
        SecurityRoleTableModel existing = getOne(new LambdaQueryWrapper<SecurityRoleTableModel>()
                .eq(SecurityRoleTableModel::getRoleId, dto.getRoleId())
                .eq(SecurityRoleTableModel::getModulePrefix, modulePrefix)
                .eq(SecurityRoleTableModel::getDatasource, datasource)
                .eq(SecurityRoleTableModel::getTableName, dto.getTableName()));

        if (existing != null) {
            existing.setFieldConfig(fieldConfigMap);
            return updateById(existing);
        }

        SecurityRoleTableModel entity = new SecurityRoleTableModel();
        entity.setRoleId(dto.getRoleId());
        entity.setModulePrefix(modulePrefix);
        entity.setDatasource(datasource);
        entity.setTableName(dto.getTableName());
        entity.setFieldConfig(fieldConfigMap);
        return save(entity);
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }

    @Override
    public Map<String, Map<String, FieldPermission>> getMergedRoleTableModelPermission(List<String> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Map.of();
        }
        List<SecurityRoleTableModel> roleTableModels = list(new LambdaQueryWrapper<SecurityRoleTableModel>()
                .in(SecurityRoleTableModel::getRoleId, roleIds));

        // 按 "module_prefix:datasource:table_name" 分组
        Map<String, List<SecurityRoleTableModel>> grouped = roleTableModels.stream()
                .collect(Collectors.groupingBy(m -> m.getModulePrefix() + ":" + m.getDatasource() + ":" + m.getTableName()));

        // 多角色取最大权限：show取或，desensitize取与
        Map<String, Map<String, FieldPermission>> result = new HashMap<>();
        for (Map.Entry<String, List<SecurityRoleTableModel>> entry : grouped.entrySet()) {
            Map<String, FieldPermission> mergedFields = new HashMap<>();
            for (SecurityRoleTableModel rtm : entry.getValue()) {
                if (rtm.getFieldConfig() != null) {
                    mergeFieldPermissions(mergedFields, rtm.getFieldConfig());
                }
            }
            result.put(entry.getKey(), mergedFields);
        }
        return result;
    }

    /**
     * 合并字段权限：show取或（任一角色允许则允许），desensitize取与（所有角色都脱敏才脱敏）
     */
    private void mergeFieldPermissions(Map<String, FieldPermission> merged, Map<String, FieldPermission> incoming) {
        for (Map.Entry<String, FieldPermission> entry : incoming.entrySet()) {
            String fieldName = entry.getKey();
            FieldPermission incomingPerm = entry.getValue();
            FieldPermission existingPerm = merged.get(fieldName);

            if (existingPerm == null) {
                merged.put(fieldName, incomingPerm);
            } else {
                boolean showResult = existingPerm.show() || incomingPerm.show();
                boolean desensitizeResult = existingPerm.desensitize() && incomingPerm.desensitize();
                SensitiveStrategy strategyResult = desensitizeResult
                        ? (existingPerm.strategy() != SensitiveStrategy.NONE ? existingPerm.strategy() : incomingPerm.strategy())
                        : SensitiveStrategy.NONE;
                Integer prefixResult = desensitizeResult
                        ? (existingPerm.prefixNoMaskLen() != null ? existingPerm.prefixNoMaskLen() : incomingPerm.prefixNoMaskLen())
                        : null;
                Integer suffixResult = desensitizeResult
                        ? (existingPerm.suffixNoMaskLen() != null ? existingPerm.suffixNoMaskLen() : incomingPerm.suffixNoMaskLen())
                        : null;
                String symbolResult = desensitizeResult
                        ? (existingPerm.symbol() != null ? existingPerm.symbol() : incomingPerm.symbol())
                        : null;
                merged.put(fieldName, new FieldPermission(showResult, desensitizeResult, strategyResult, prefixResult, suffixResult, symbolResult));
            }
        }
    }
}
