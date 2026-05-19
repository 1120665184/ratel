package org.quyq.gwsu.security.apiresource.service.impl;

import cn.hutool.crypto.digest.MD5;
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
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiTableModelMapper;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiTableModelService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityApiTableModelServiceImpl extends ServiceImpl<SecurityApiTableModelMapper, SecurityApiTableModel>
        implements ISecurityApiTableModelService {

    @Override
    public IPage<TableModelVO> pageByCondition(TableModelQueryDTO query) {
        Page<SecurityApiTableModel> page = Page.of(query.getPageNum(), query.getPageSize());
        return getBaseMapper().pageByCondition(page, query).convert(SecurityApiTableModel::toVo);
    }

    @Override
    public List<SecurityApiTableModel> listByCondition(TableModelQueryDTO query) {
        return getBaseMapper().listTableModelByCondition(query);
    }

    @Override
    public List<TableModelVO> listByApiId(String apiId) {
        TableModelQueryDTO form = new TableModelQueryDTO();
        form.setApiId(apiId);
        return getBaseMapper().listTableModelByCondition(form)
                .stream().map(SecurityApiTableModel::toVo).toList();
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

            TableModelQueryDTO form = new TableModelQueryDTO();
            form.setApiIds(batch);
            allVos.addAll(getBaseMapper().listTableModelByCondition(form)
                    .stream().map(SecurityApiTableModel::toVo).toList());
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
        TableModelQueryDTO form = new TableModelQueryDTO();
        form.setModulePrefix(modulePrefix);
        return getBaseMapper().listTableModelByCondition(form)
                .stream().map(SecurityApiTableModel::toVo).toList();
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
