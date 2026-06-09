package org.quyq.gwsu.security.role.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.SensitiveStrategy;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.role.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.role.vo.RolePermissionTableModelVO;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModel;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiResourceService;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiTableModelService;
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
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelColumnService;
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
    private final ISecurityApiTableModelService apiTableModelService;
    private final ISecurityApiResourceService apiResourceService;
    private final ISecurityTableModelTableService modelTableService;
    private final ISecurityTableModelColumnService modelColumnService;

    private static final int BATCH_SIZE = 500;

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
            if (dto.getEnabled() != null) {
                existing.setEnabled(dto.getEnabled());
            }
            return updateById(existing);
        }

        SecurityRoleTableModel entity = new SecurityRoleTableModel();
        entity.setRoleId(dto.getRoleId());
        entity.setModulePrefix(modulePrefix);
        entity.setDatasource(datasource);
        entity.setTableName(dto.getTableName());
        entity.setFieldConfig(fieldConfigMap);
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
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
        Map<String, Map<String, FieldPermission>> result = new HashMap<>();

        List<SecurityRoleTableModel> roleTableModels = list(new LambdaQueryWrapper<SecurityRoleTableModel>()
                .in(SecurityRoleTableModel::getRoleId, roleIds));

        Set<String> disabledTableKeys = new HashSet<>();
        Map<String, List<SecurityRoleTableModel>> grouped = roleTableModels.stream()
                .collect(Collectors.groupingBy(m -> m.getModulePrefix() + ":" + m.getDatasource() + ":" + m.getTableName()));

        for (Map.Entry<String, List<SecurityRoleTableModel>> entry : grouped.entrySet()) {
            List<SecurityRoleTableModel> records = entry.getValue();
            boolean anyEnabled = records.stream().anyMatch(r -> r.getEnabled() == null || r.getEnabled());
            if (!anyEnabled) {
                disabledTableKeys.add(entry.getKey());
            }
        }

        if (isAdmin) {
            modelTableService.list(new LambdaQueryWrapper<>(SecurityTableModelTable.class)
                            .eq(SecurityTableModelTable::getSourceType, 1)
                            .eq(SecurityTableModelTable::getDeleted, false)
                    ).stream().map(v -> v.getModulePrefix() + ":" + v.getDataSource() + ":" + v.getTableName())
                    .forEach(table -> result.put(table, Map.of()));
        } else {
            for (Map.Entry<String, List<SecurityRoleTableModel>> entry : grouped.entrySet()) {
                if (disabledTableKeys.contains(entry.getKey())) {
                    continue;
                }
                Map<String, FieldPermission> mergedFields = new HashMap<>();
                for (SecurityRoleTableModel rtm : entry.getValue()) {
                    if (rtm.getEnabled() != null && !rtm.getEnabled()) {
                        continue;
                    }
                    if (rtm.getFieldConfig() != null) {
                        mergeFieldPermissions(mergedFields, rtm.getFieldConfig());
                    }
                }
                result.put(entry.getKey(), mergedFields);
            }
        }

        List<String> apiIds = getApiIdsByRoleIds(roleIds, isAdmin);
        if (!CollectionUtils.isEmpty(apiIds)) {
            List<SecurityApiTableModel> apiTableModels = listApiTableModelsByApiIds(apiIds);
            if (!CollectionUtils.isEmpty(apiTableModels)) {
                Map<String, SecurityApiTableModel> apiGroupd = apiTableModels.stream().collect(Collectors.toMap(m -> m.getModulePrefix() + ":" + m.getDatasource() + ":" + m.getTableName(),
                        Function.identity(), (k1, k2) -> k2));
                for (Map.Entry<String, SecurityApiTableModel> entry : apiGroupd.entrySet()) {
                    String key = entry.getKey();
                    if (disabledTableKeys.contains(key)) {
                        continue;
                    }
                    Map<String, FieldPermission> existingFields = result.computeIfAbsent(key, k -> new HashMap<>());
                    SecurityApiTableModel atm = entry.getValue();
                    if (Objects.nonNull(atm)) {
                        mergeApiWithRoleFieldPermissions(existingFields, atm.getFieldConfig());
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<RolePermissionTableModelVO> getTableModelPermission(String roleId) {
        SecurityRole role = roleMapper.selectById(roleId);
        if (Objects.isNull(role)) {
            return Collections.emptyList();
        }

        boolean isAdmin = SecurityConstants.Authentication.ROLE_SUPER_ADMIN_FLAG.equals(role.getRoleCode());

        List<String> apiIds = getApiIdsByRoleIds(Collections.singletonList(roleId), isAdmin);
        if (CollectionUtils.isEmpty(apiIds)) {
            return Collections.emptyList();
        }
        List<String> tableIds = listApiTableModelsByApiIds(apiIds).stream()
                .map(v -> SecurityTableModelTable.genId(v.getModulePrefix(), v.getDatasource(), v.getTableName()))
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(tableIds)) {
            return Collections.emptyList();
        }

        List<SecurityTableModelTable> tables = listTableModelTableByIds(tableIds);
        Map<String, List<SecurityTableModelColumn>> columns = tableModelColumnByTableIds(tableIds);
        List<RolePermissionTableModelVO> datas = buildPermissionTableModel(tables, columns);

        Map<String, SecurityRoleTableModel> roleTableModelMap = list(new LambdaQueryWrapper<SecurityRoleTableModel>()
                .eq(SecurityRoleTableModel::getRoleId, roleId))
                .stream().collect(Collectors.toMap(v -> SecurityTableModelTable.genId(v.getModulePrefix(), v.getDatasource(), v.getTableName()), Function.identity()));

        for (RolePermissionTableModelVO vo : datas) {
            SecurityRoleTableModel rtm = roleTableModelMap.remove(vo.getTableModelId());
            if (rtm != null) {
                vo.setId(rtm.getId());
                vo.setEnabled(rtm.getEnabled() != null ? rtm.getEnabled() : true);
                Map<String, FieldPermission> fieldConfig = rtm.getFieldConfig();
                if (!CollectionUtils.isEmpty(fieldConfig)) {
                    vo.getColumns().forEach(column -> {
                        FieldPermission fieldPermission = fieldConfig.get(column.getColumnName());
                        if (Objects.nonNull(fieldPermission)) {
                            column.setCustomFieldConfig(buildFieldConfigItem(column.getColumnName(), fieldPermission));
                        }
                    });
                }
            }
        }

        if (isAdmin) {
            List<SecurityTableModelTable> customDatas = modelTableService.list(new LambdaQueryWrapper<>(SecurityTableModelTable.class)
                    .eq(SecurityTableModelTable::getSourceType, 1)
                    .eq(SecurityTableModelTable::getDeleted, false)
            );
            if (!CollectionUtils.isEmpty(customDatas)) {
                Map<String, List<SecurityTableModelColumn>> customColumns = tableModelColumnByTableIds(customDatas.stream().map(SecurityTableModelTable::getId).toList());
                List<RolePermissionTableModelVO> customVOs = buildPermissionTableModel(customDatas, customColumns);
                customVOs.forEach(vo -> vo.setType(1));
                datas.addAll(customVOs);
            }
        } else {
            if (!roleTableModelMap.isEmpty()) {
                List<String> tIds = new ArrayList<>(roleTableModelMap.keySet());
                List<RolePermissionTableModelVO> cusD = buildPermissionTableModel(
                        listTableModelTableByIds(tIds),
                        tableModelColumnByTableIds(tIds));
                cusD.forEach(model -> {
                    SecurityRoleTableModel m = roleTableModelMap.get(model.getTableModelId());
                    model.setType(1);
                    model.setId(m.getId());
                    model.setEnabled(m.getEnabled() != null ? m.getEnabled() : true);
                    Map<String, FieldPermission> fieldConfig = m.getFieldConfig();
                    if (!CollectionUtils.isEmpty(fieldConfig)) {
                        model.getColumns().forEach(column -> {
                            FieldPermission fieldPermission = fieldConfig.get(column.getColumnName());
                            if (Objects.nonNull(fieldPermission)) {
                                column.setCustomFieldConfig(buildFieldConfigItem(column.getColumnName(), fieldPermission));
                            }
                        });
                    }
                });
                datas.addAll(cusD);
            }
        }

        return datas;
    }

    private RoleTableModelSaveDTO.FieldConfigItem buildFieldConfigItem(String columnName ,FieldPermission field){
        if(Objects.isNull(field)){
            return null;
        }
        RoleTableModelSaveDTO.FieldConfigItem item = new RoleTableModelSaveDTO.FieldConfigItem();
        item.setFieldName(columnName)
                .setShow(field.show())
                .setDesensitize(field.desensitize())
                .setStrategy(field.strategy().name())
                .setPrefixNoMaskLen(field.prefixNoMaskLen())
                .setSuffixNoMaskLen(field.suffixNoMaskLen())
                .setSymbol(field.symbol());

        return item;
    }


    private List<RolePermissionTableModelVO> buildPermissionTableModel(List<SecurityTableModelTable> tables, Map<String, List<SecurityTableModelColumn>> columns) {
        if (CollectionUtils.isEmpty(tables)) {
            return new ArrayList<>();
        }

        return tables.stream()
                .map(t -> {
                    RolePermissionTableModelVO tmp = new RolePermissionTableModelVO();
                    tmp.setTableModelId(t.getId())
                            .setTableName(t.getTableName())
                            .setModulePrefix(t.getModulePrefix())
                            .setDatasource(t.getDataSource())
                            .setTableComment(t.getTableComment());
                    List<SecurityTableModelColumn> cs = columns.get(t.getId());
                    if (!CollectionUtils.isEmpty(cs)) {
                        tmp.setColumns(
                                cs.stream().map(c -> {
                                    RolePermissionTableModelVO.ColumnInfo columnInfo = new RolePermissionTableModelVO.ColumnInfo();
                                    columnInfo.setColumnName(c.getColumnName())
                                            .setColumnComment(c.getColumnComment())
                                            .setFixedFieldConfig(buildFieldConfigItem(c.getColumnName() , c.getFieldConfig()));
                                    return columnInfo;
                                }).toList()
                        );
                    }

                    return tmp;
                }).collect(Collectors.toList());

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
            TableModelQueryDTO form = new TableModelQueryDTO();
            form.setApiIds(batch);
            all.addAll(apiTableModelService.listByCondition(form));
        }
        return all;
    }

    /**
     * 获取所有表模型
     *
     * @param tableIds
     * @return
     */
    private List<SecurityTableModelTable> listTableModelTableByIds(List<String> tableIds) {
        if (CollectionUtils.isEmpty(tableIds)) {
            return Collections.emptyList();
        }
        List<SecurityTableModelTable> all = new ArrayList<>();
        for (int i = 0; i < tableIds.size(); i += BATCH_SIZE) {
            List<String> batch = tableIds.subList(i, Math.min(i + BATCH_SIZE, tableIds.size()));
            all.addAll(modelTableService.listByIds(batch));
        }
        return all;
    }

    /**
     * 获取列数据
     *
     * @param tableIds
     * @return
     */
    private Map<String, List<SecurityTableModelColumn>> tableModelColumnByTableIds(List<String> tableIds) {
        if (CollectionUtils.isEmpty(tableIds)) {
            return Collections.emptyMap();
        }
        List<SecurityTableModelColumn> all = new ArrayList<>();
        for (int i = 0; i < tableIds.size(); i += BATCH_SIZE) {
            List<String> batch = tableIds.subList(i, Math.min(i + BATCH_SIZE, tableIds.size()));
            all.addAll(modelColumnService.list(new LambdaQueryWrapper<>(SecurityTableModelColumn.class)
                    .in(SecurityTableModelColumn::getTableId, batch)));
        }
        return all.stream().collect(Collectors.groupingBy(SecurityTableModelColumn::getTableId));

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
