package org.quyq.gwsu.security.tablemodel.service.impl;

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
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityApiTableModel;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityApiTableModelMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityApiTableModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityApiTableModelServiceImpl extends ServiceImpl<SecurityApiTableModelMapper, SecurityApiTableModel>
        implements ISecurityApiTableModelService {

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
        return page(page, wrapper).convert(SecurityApiTableModel::toVo);
    }

    @Override
    public List<TableModelVO> listByApiId(String apiId) {
        return list(new LambdaQueryWrapper<SecurityApiTableModel>()
                .eq(SecurityApiTableModel::getApiId, apiId))
                .stream()
                .map(SecurityApiTableModel::toVo)
                .toList();
    }

    @Override
    public List<TableModelVO> listByModulePrefix(String modulePrefix) {
        return list(new LambdaQueryWrapper<SecurityApiTableModel>()
                .eq(SecurityApiTableModel::getModulePrefix, modulePrefix))
                .stream()
                .map(SecurityApiTableModel::toVo)
                .toList();
    }

    @Override
    @Transactional
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
