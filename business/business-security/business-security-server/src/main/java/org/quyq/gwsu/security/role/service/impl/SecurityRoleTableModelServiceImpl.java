package org.quyq.gwsu.security.role.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.role.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.role.vo.RoleTableModelVO;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModel;
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiTableModelMapper;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiResourceService;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.role.domain.SecurityRole;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenuPermission;
import org.quyq.gwsu.security.role.domain.SecurityRoleTableModel;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuPermissionMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleTableModelMapper;
import org.quyq.gwsu.security.role.service.ISecurityRoleTableModelService;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityRoleTableModelServiceImpl extends ServiceImpl<SecurityRoleTableModelMapper, SecurityRoleTableModel>
        implements ISecurityRoleTableModelService {

    private final SecurityRoleMenuMapper roleMenuMapper;
    private final SecurityRoleMapper roleMapper;
    private final SecurityRoleMenuPermissionMapper roleMenuPermissionMapper;
    private final SecurityApiTableModelMapper apiTableModelMapper;
    private final ISecurityApiResourceService apiResourceService;
    private final ISecurityTableModelTableService modelTableService;

    private static final int BATCH_SIZE = 500;

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
    public Map<String, Map<String, FieldPermission>> getMergedRoleTableModelPermission(List<String> roleCodes) {
        if (CollectionUtils.isEmpty(roleCodes)) {
            return Map.of();
        }
        boolean isAdmin = false;
        if (roleCodes.contains(SecurityConstants.Authentication.ROLE_SUPER_ADMIN_FLAG)) {
            isAdmin = true;
        }
        List<String> roleIds = roleMapper.selectList(new LambdaQueryWrapper<SecurityRole>()
                        .in(SecurityRole::getRoleCode, roleCodes))
                .stream().map(SecurityRole::getId)
                .toList();
        if (CollUtil.isEmpty(roleIds)) {
            return Map.of();
        }
        // 多角色取最大权限：show取或，desensitize取与
        Map<String, Map<String, FieldPermission>> result = new HashMap<>();

        if (isAdmin) {
            modelTableService.list(new LambdaQueryWrapper<>(SecurityTableModelTable.class)
                            //获取所有自定义添加的表模型
                            .eq(SecurityTableModelTable::getSourceType, 1)
                            .eq(SecurityTableModelTable::getDeleted, 0)
                    ).stream().map(v -> v.getModulePrefix() + ":" + v.getDataSource() + ":" + v.getTableName())
                    .forEach(table -> result.put(table, Map.of()));
        } else {
            List<SecurityRoleTableModel> roleTableModels = list(new LambdaQueryWrapper<SecurityRoleTableModel>()
                    .in(SecurityRoleTableModel::getRoleId, roleIds));

            // 按 "module_prefix:datasource:table_name" 分组
            Map<String, List<SecurityRoleTableModel>> grouped = roleTableModels.stream()
                    .collect(Collectors.groupingBy(m -> m.getModulePrefix() + ":" + m.getDatasource() + ":" + m.getTableName()));

            for (Map.Entry<String, List<SecurityRoleTableModel>> entry : grouped.entrySet()) {
                Map<String, FieldPermission> mergedFields = new HashMap<>();
                for (SecurityRoleTableModel rtm : entry.getValue()) {
                    if (rtm.getFieldConfig() != null) {
                        mergeFieldPermissions(mergedFields, rtm.getFieldConfig());
                    }
                }
                result.put(entry.getKey(), mergedFields);
            }
        }

        // 通过角色关联的菜单获取接口绑定的表模型权限，与角色自定义权限合并
        List<String> apiIds = getApiIdsByRoleIds(roleIds, isAdmin);
        if (!CollectionUtils.isEmpty(apiIds)) {
            List<SecurityApiTableModel> apiTableModels = listApiTableModelsByApiIds(apiIds);
            if (!CollectionUtils.isEmpty(apiTableModels)) {
                // 按 "module_prefix:datasource:table_name" 分组
                Map<String, SecurityApiTableModel> apiGroupd = apiTableModels.stream().collect(Collectors.toMap(m -> m.getModulePrefix() + ":" + m.getDatasource() + ":" + m.getTableName(),
                        Function.identity(), (k1, k2) -> k2));
                for (Map.Entry<String, SecurityApiTableModel> entry : apiGroupd.entrySet()) {
                    String key = entry.getKey();
                    Map<String, FieldPermission> existingFields = result.computeIfAbsent(key, k -> new HashMap<>());
                    // 合并接口绑定的字段权限（接口优先级高）
                    SecurityApiTableModel atm = entry.getValue();
                    if (Objects.nonNull(atm)) {
                        mergeApiWithRoleFieldPermissions(existingFields, atm.getFieldConfig());
                    }
                }
            }
        }

        return result;
    }

    /**
     * 通过角色ID列表获取关联的所有接口资源ID
     * roleIds → security_role_menu → security_role_menu_permission → apiIds
     */
    private List<String> getApiIdsByRoleIds(List<String> roleIds, boolean isAdmin) {
        //管理员直接返回所有的接口API
        if (isAdmin) {
            return apiResourceService.list(
                    new LambdaQueryWrapper<SecurityApiResource>()
                            .eq(SecurityApiResource::getDeleted, 0)
            ).stream().map(SecurityApiResource::getId).toList();
        }

        // 查询角色关联的菜单
        List<SecurityRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .in(SecurityRoleMenu::getRoleId, roleIds));
        if (CollectionUtils.isEmpty(roleMenus)) {
            return Collections.emptyList();
        }

        List<String> roleMenuIds = roleMenus.stream()
                .map(SecurityRoleMenu::getId)
                .toList();

        // 查询菜单关联的接口权限
        List<SecurityRoleMenuPermission> permissions = roleMenuPermissionMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenuPermission>()
                        .in(SecurityRoleMenuPermission::getRoleMenuId, roleMenuIds));
        if (CollectionUtils.isEmpty(permissions)) {
            return Collections.emptyList();
        }

        return permissions.stream()
                .map(SecurityRoleMenuPermission::getApiId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 批量查询接口绑定的表模型（支持大量apiId分批查询）
     */
    private List<SecurityApiTableModel> listApiTableModelsByApiIds(List<String> apiIds) {
        if (CollectionUtils.isEmpty(apiIds)) {
            return Collections.emptyList();
        }
        List<SecurityApiTableModel> all = new ArrayList<>();
        for (int i = 0; i < apiIds.size(); i += BATCH_SIZE) {
            List<String> batch = apiIds.subList(i, Math.min(i + BATCH_SIZE, apiIds.size()));
            all.addAll(apiTableModelMapper.listTableModelByApiId(batch));
        }
        return all;
    }

    /**
     * 合并接口绑定的字段权限到已有权限中
     * 接口关联的表模型优先级高：
     * - 注解标注 show=false → 无论角色如何配置，该字段不可查询
     * - 注解标注 desensitize=true → 无论角色如何配置，该字段必须脱敏
     */
    private void mergeApiWithRoleFieldPermissions(Map<String, FieldPermission> existing, Map<String, FieldPermission> incoming) {
        for (Map.Entry<String, FieldPermission> entry : incoming.entrySet()) {
            String fieldName = entry.getKey();
            FieldPermission apiPerm = entry.getValue();
            FieldPermission existingPerm = existing.get(fieldName);

            if (existingPerm == null) {
                // 字段只存在于接口配置中，直接使用
                existing.put(fieldName, apiPerm);
            } else {
                // 两边都有配置，接口优先级高
                // show: 如果接口配置不可见，则不可见（强制约束）
                boolean showResult = apiPerm.show() && existingPerm.show();
                // desensitize: 如果接口配置要求脱敏，则必须脱敏（强制约束）
                boolean desensitizeResult = apiPerm.desensitize() || existingPerm.desensitize();
                // 脱敏策略：接口要求脱敏时优先使用接口配置，否则使用角色配置
                SensitiveStrategy strategyResult = apiPerm.desensitize()
                        ? (apiPerm.strategy() != SensitiveStrategy.NONE ? apiPerm.strategy() : existingPerm.strategy())
                        : existingPerm.strategy();
                Integer prefixResult = apiPerm.desensitize()
                        ? (apiPerm.prefixNoMaskLen() != null ? apiPerm.prefixNoMaskLen() : existingPerm.prefixNoMaskLen())
                        : existingPerm.prefixNoMaskLen();
                Integer suffixResult = apiPerm.desensitize()
                        ? (apiPerm.suffixNoMaskLen() != null ? apiPerm.suffixNoMaskLen() : existingPerm.suffixNoMaskLen())
                        : existingPerm.suffixNoMaskLen();
                String symbolResult = apiPerm.desensitize()
                        ? (apiPerm.symbol() != null ? apiPerm.symbol() : existingPerm.symbol())
                        : existingPerm.symbol();
                existing.put(fieldName, new FieldPermission(showResult, desensitizeResult, strategyResult, prefixResult, suffixResult, symbolResult));
            }
        }
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
