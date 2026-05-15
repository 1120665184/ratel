package org.quyq.gwsu.security.apiresource.service.impl;

import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.security.collector.ApiEndpointCollector;
import org.quyq.gwsu.common.security.domain.ApiEndpointInfo;
import org.quyq.gwsu.common.security.domain.TableModelInfo;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.apiresource.vo.TableModelVO;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModel;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModelConfig;
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiTableModelConfigMapper;
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiTableModelMapper;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiTableModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityApiTableModelServiceImpl extends ServiceImpl<SecurityApiTableModelMapper, SecurityApiTableModel>
        implements ISecurityApiTableModelService {

    private final SecurityApiTableModelConfigMapper configMapper;

    @Override
    public IPage<TableModelVO> pageByCondition(TableModelQueryDTO query) {
        Page<SecurityApiTableModel> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SecurityApiTableModel> wrapper = new LambdaQueryWrapper<>();
        if (query.getModulePrefix() != null) {
            wrapper.eq(SecurityApiTableModel::getModulePrefix, query.getModulePrefix());
        }
        if (query.getTableName() != null) {
            wrapper.like(SecurityApiTableModel::getTableName, query.getTableName());
        }
        if (query.getApiId() != null) {
            wrapper.eq(SecurityApiTableModel::getApiId, query.getApiId());
        }
        IPage<TableModelVO> result = page(page, wrapper).convert(SecurityApiTableModel::toVo);
        applyConfigOverride(result.getRecords());
        return result;
    }

    @Override
    public List<TableModelVO> listByApiId(String apiId) {
        List<TableModelVO> list = list(new LambdaQueryWrapper<SecurityApiTableModel>()
                .eq(SecurityApiTableModel::getApiId, apiId))
                .stream()
                .map(SecurityApiTableModel::toVo)
                .toList();
        return applyConfigOverride(list);
    }

    private static final int BATCH_SIZE = 500;

    @Override
    public List<TableModelVO> listByApiIds(Collection<String> apiIds) {
        if (CollectionUtils.isEmpty(apiIds)) {
            return Collections.emptyList();
        }
        List<String> apiIdList = apiIds instanceof List<String> l ? l : new ArrayList<>(apiIds);
        // 分批查询并回填 config 覆盖值，避免 IN 子句超限
        List<TableModelVO> allVos = new ArrayList<>();
        for (int i = 0; i < apiIdList.size(); i += BATCH_SIZE) {
            List<String> batch = apiIdList.subList(i, Math.min(i + BATCH_SIZE, apiIdList.size()));
            List<TableModelVO> batchVos = list(new LambdaQueryWrapper<SecurityApiTableModel>()
                            .in(SecurityApiTableModel::getApiId, batch))
                    .stream()
                    .map(SecurityApiTableModel::toVo)
                    .collect(Collectors.toCollection(ArrayList::new));
            applyConfigOverride(batchVos);
            allVos.addAll(batchVos);
        }
        if (allVos.isEmpty()) {
            return Collections.emptyList();
        }
        // 按覆盖后的 modulePrefix+datasource+tableName 去重
        Map<String, TableModelVO> dedupMap = new LinkedHashMap<>();
        for (TableModelVO vo : allVos) {
            String key = "%s:%s:%s".formatted(vo.getModulePrefix(), vo.getDatasource(), vo.getTableName());
            dedupMap.putIfAbsent(key, vo);
        }
        return new ArrayList<>(dedupMap.values());
    }

    @Override
    public List<TableModelVO> listByModulePrefix(String modulePrefix) {
        List<TableModelVO> list = list(new LambdaQueryWrapper<SecurityApiTableModel>()
                .eq(SecurityApiTableModel::getModulePrefix, modulePrefix))
                .stream()
                .map(SecurityApiTableModel::toVo)
                .toList();
        return applyConfigOverride(list);
    }

    /**
     * 根据 security_api_table_model_config 覆盖数据源配置
     * 优先级：config 中的 datasource > 采集默认的 datasource
     */
    private List<TableModelVO> applyConfigOverride(List<TableModelVO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        // 批量查询所有关联的 config 记录
        List<String> tableModelIds = list.stream()
                .map(TableModelVO::getId)
                .toList();
        Map<String, SecurityApiTableModelConfig> configMap = configMapper.selectList(
                        new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                                .in(SecurityApiTableModelConfig::getTableModelId, tableModelIds))
                .stream()
                .collect(Collectors.toMap(
                        SecurityApiTableModelConfig::getTableModelId,
                        c -> c,
                        (a, b) -> a));
        // 回填覆盖值
        for (TableModelVO vo : list) {
            SecurityApiTableModelConfig config = configMap.get(vo.getId());
            if (config != null) {
                vo.setDatasource(config.getDatasource());
            }
        }
        return list;
    }

    @Override
    public void handleTableModel(String applicationName, ApiEndpointCollector.ApiEndpointWrapper permissions) {
        // 从接口数据中提取所有表模型绑定关系
        List<SecurityApiTableModel> newTableModels = new ArrayList<>();
        for (Map.Entry<String, List<ApiEndpointInfo>> entry : permissions.endpoints().entrySet()) {
            for (ApiEndpointInfo endpointInfo : entry.getValue()) {
                if (endpointInfo.tableModels() == null || endpointInfo.tableModels().isEmpty()) {
                    continue;
                }
                for (TableModelInfo tableModelInfo : endpointInfo.tableModels()) {
                    String id = MD5.create().digestHex(
                            "%s:%s:%s:%s".formatted(
                                    tableModelInfo.modulePrefix(),
                                    tableModelInfo.datasource(),
                                    tableModelInfo.tableName(),
                                    endpointInfo.id()));
                    SecurityApiTableModel model = new SecurityApiTableModel();
                    model.setId(id);
                    model.setApiId(endpointInfo.id());
                    model.setModulePrefix(tableModelInfo.modulePrefix());
                    model.setDatasource(tableModelInfo.datasource());
                    model.setTableName(tableModelInfo.tableName());
                    model.setFieldConfig(tableModelInfo.fieldConfig());
                    newTableModels.add(model);
                }
            }
        }

        // 查询旧数据
        Set<String> modules = permissions.endpoints().keySet();
        List<SecurityApiTableModel> oldTableModels = lambdaQuery()
                .in(SecurityApiTableModel::getModulePrefix, modules)
                .list();

        // 判断是否有变动
        boolean hasChanged = isTableModelChanged(newTableModels, oldTableModels);
        if (!hasChanged) {
            return;
        }

        log.info("模块 {} 的表模型绑定有变动，旧数据: {}, 新数据: {}", applicationName, oldTableModels.size(), newTableModels.size());

        // 删除旧数据
        if (!CollectionUtils.isEmpty(oldTableModels)) {
            List<String> oldIds = oldTableModels.stream().map(SecurityApiTableModel::getId).toList();
            removeByIds(oldIds);
        }

        // 插入新数据
        if (!CollectionUtils.isEmpty(newTableModels)) {
            saveBatch(newTableModels);
        }
    }

    private boolean isTableModelChanged(List<SecurityApiTableModel> newModels, List<SecurityApiTableModel> oldModels) {
        if (newModels.size() != oldModels.size()) {
            return true;
        }
        Map<String, SecurityApiTableModel> oldMap = new HashMap<>();
        for (SecurityApiTableModel m : oldModels) {
            oldMap.put(m.getId(), m);
        }
        for (SecurityApiTableModel newModel : newModels) {
            SecurityApiTableModel oldModel = oldMap.get(newModel.getId());
            if (oldModel == null || !Objects.equals(newModel, oldModel)) {
                return true;
            }
        }
        return false;
    }
}
