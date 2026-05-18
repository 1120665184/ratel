package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.ForeignKeyInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelChangeDatasourceDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelCollectDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelCustomSaveDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.TableModelTableQueryDTO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelForeignKeyVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModel;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiTableModelConfig;
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiTableModelConfigMapper;
import org.quyq.gwsu.security.apiresource.service.ISecurityApiTableModelService;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityTableModelTableMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelColumnService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelForeignKeyService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表基本信息 服务实现
 *
 * @author Quyq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityTableModelTableServiceImpl extends ServiceImpl<SecurityTableModelTableMapper, SecurityTableModelTable>
        implements ISecurityTableModelTableService {

    private final ISecurityTableModelColumnService securityTableModelColumnService;

    private final ISecurityTableModelForeignKeyService securityTableModelForeignKeyService;

    private final RestClient.Builder clientBuilder;

    private final ISQLExecutionService sqlExecutionService;

    private final ISecurityApiTableModelService apiTableModelService;

    private final SecurityApiTableModelConfigMapper configMapper;

    private final List<BusinessModuleInfoProvider> providers;

    @Override
    public TableModelTableVO getById(String id) {
        SecurityTableModelTable entity = super.getById(id);
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public TableModelDetailVO getTableDetail(String modulePrefix, String datasource, String tableName) {
        // 查询所有匹配的表
        SecurityTableModelTable table = getOne(
                new LambdaQueryWrapper<SecurityTableModelTable>()
                        .eq(SecurityTableModelTable::getModulePrefix, modulePrefix)
                        .eq(SecurityTableModelTable::getDataSource, datasource)
                        .eq(SecurityTableModelTable::getTableName, tableName)
                        .eq(SecurityTableModelTable::getDeleted, false));

        if (Objects.isNull(table)) {
            return null;
        }

        // 批量查询字段
        List<SecurityTableModelColumn> columns = securityTableModelColumnService.list(
                new LambdaQueryWrapper<SecurityTableModelColumn>()
                        .eq(SecurityTableModelColumn::getTableId, table.getId())
                        .eq(SecurityTableModelColumn::getDeleted, false)
                        .orderByAsc(SecurityTableModelColumn::getOrdinalPosition));

        // 批量查询外键
        List<SecurityTableModelForeignKey> foreignKeys = securityTableModelForeignKeyService.list(
                new LambdaQueryWrapper<SecurityTableModelForeignKey>()
                        .eq(SecurityTableModelForeignKey::getTableId, table.getId())
                        .eq(SecurityTableModelForeignKey::getDeleted, false));

        TableModelDetailVO detail = new TableModelDetailVO();
        detail.setTable(table.toVo());
        detail.setColumns(columns.stream().map(SecurityTableModelColumn::toVo).toList());
        detail.setForeignKeys(foreignKeys.stream().map(SecurityTableModelForeignKey::toVo).toList());

        return detail;
    }

    @Override
    public TableModelTableVO getByTableNameAndDataSource(String tableName, String dataSource) {
        SecurityTableModelTable entity = getOne(new LambdaQueryWrapper<SecurityTableModelTable>()
                .eq(SecurityTableModelTable::getTableName, tableName)
                .eq(SecurityTableModelTable::getDataSource, dataSource)
                .eq(SecurityTableModelTable::getDeleted, false));
        return entity != null ? entity.toVo() : null;
    }

    @Override
    public List<TableModelTableVO> listAll() {
        return list(new LambdaQueryWrapper<SecurityTableModelTable>()
                .eq(SecurityTableModelTable::getDeleted, false))
                .stream()
                .map(SecurityTableModelTable::toVo)
                .toList();
    }

    @Override
    public Boolean saveOrUpdateTable(TableModelTableVO vo) {
        SecurityTableModelTable entity = SecurityTableModelTable.toDo(vo);
        return saveOrUpdate(entity);
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        return removeBatchByIds(ids);
    }

    @Override
    public List<TableInfo> tableList(String applicationName, String datasource) {
        if (DeployUtils.isSingle()) {
            return sqlExecutionService.tableList(datasource);
        }
        RestClient restClient = clientBuilder.clone()
                .baseUrl("http://%s".formatted(applicationName))
                .build();

        String uri = CoreConstants.EndPoint.ENDPOINT_DB_TABLES;
        if (StringUtils.isNotBlank(datasource)) {
            uri += "?datasource=%s".formatted(datasource);
        }
        return FeignUtils.data(restClient
                .get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }));
    }

    @Override
    public List<ColumnInfo> columnList(String applicationName, String datasource, String tableName) {
        if (DeployUtils.isSingle()) {
            return sqlExecutionService.columnList(datasource, tableName);
        }

        RestClient restClient = clientBuilder.clone()
                .baseUrl("http://%s".formatted(applicationName))
                .build();

        String uri = CoreConstants.EndPoint.ENDPOINT_DB_COLUMNS + "?tableName=%s".formatted(tableName);
        if (StringUtils.isNotBlank(datasource)) {
            uri += "&datasource=%s".formatted(datasource);
        }
        return FeignUtils.data(restClient
                .get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }));
    }

    @Override
    public IPage<TableModelTableVO> pageByCondition(TableModelTableQueryDTO query) {
        Page<SecurityTableModelTable> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<SecurityTableModelTable> wrapper = new LambdaQueryWrapper<>();
        // 表名模糊查询
        if (StringUtils.isNotBlank(query.getTableName())) {
            wrapper.like(SecurityTableModelTable::getTableName, query.getTableName());
        }
        // 模块前缀精确匹配
        if (StringUtils.isNotBlank(query.getModulePrefix())) {
            wrapper.eq(SecurityTableModelTable::getModulePrefix, query.getModulePrefix());
        }
        // 数据源精确匹配
        if (StringUtils.isNotBlank(query.getDataSource())) {
            wrapper.eq(SecurityTableModelTable::getDataSource, query.getDataSource());
        }
        // 来源类型精确匹配
        if (query.getSourceType() != null) {
            wrapper.eq(SecurityTableModelTable::getSourceType, query.getSourceType());
        }
        // 未删除
        wrapper.eq(SecurityTableModelTable::getDeleted, false);
        // 按创建时间降序排列
        wrapper.orderByDesc(SecurityTableModelTable::getCreateTime);

        return page(page, wrapper).convert(SecurityTableModelTable::toVo);
    }

    @Override
    public List<TableModelTableVO> listUncollected(String modulePrefix) {
        // 从 security_api_table_model 按 modulePrefix 查询绑定记录
        List<SecurityApiTableModel> apiTableModels = apiTableModelService.list(
                new LambdaQueryWrapper<SecurityApiTableModel>()
                        .eq(SecurityApiTableModel::getModulePrefix, modulePrefix));

        if (CollectionUtils.isEmpty(apiTableModels)) {
            return Collections.emptyList();
        }

        // 查询所有关联的 config 记录，用于覆盖数据源
        List<String> tableModelIds = apiTableModels.stream()
                .map(SecurityApiTableModel::getId)
                .toList();
        Map<String, SecurityApiTableModelConfig> configMap = configMapper.selectList(
                        new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                                .in(SecurityApiTableModelConfig::getTableModelId, tableModelIds))
                .stream()
                .collect(Collectors.toMap(
                        SecurityApiTableModelConfig::getTableModelId,
                        c -> c,
                        (a, b) -> a));

        // 提取唯一的 (modulePrefix, datasource, tableName) 组合
        // 注意：config 表会覆盖 datasource
        Map<String, TableModelTableVO> uniqueMap = new LinkedHashMap<>();
        for (SecurityApiTableModel atm : apiTableModels) {
            String datasource = atm.getDatasource();
            // 如果 config 中有覆盖数据源，则使用覆盖的数据源
            SecurityApiTableModelConfig config = configMap.get(atm.getId());
            if (config != null && StringUtils.isNotBlank(config.getDatasource())) {
                datasource = config.getDatasource();
            }
            String key = "%s:%s:%s".formatted(atm.getModulePrefix(), datasource, atm.getTableName());
            uniqueMap.putIfAbsent(key, buildTableVO(atm.getModulePrefix(), datasource, atm.getTableName()));
        }

        // 与 security_tablemodel_tables 中已有记录做差集
        List<SecurityTableModelTable> existingTables = list(new LambdaQueryWrapper<SecurityTableModelTable>()
                .eq(SecurityTableModelTable::getModulePrefix, modulePrefix)
                .eq(SecurityTableModelTable::getDeleted, false));

        Set<String> existingKeys = existingTables.stream()
                .map(t -> "%s:%s:%s".formatted(t.getModulePrefix(), t.getDataSource(), t.getTableName()))
                .collect(Collectors.toSet());

        // 返回未采集列表
        return uniqueMap.entrySet().stream()
                .filter(entry -> !existingKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean collectTableModels(TableModelCollectDTO dto) {
        if (dto == null || CollectionUtils.isEmpty(dto.items())) {
            return false;
        }

        for (TableModelCollectDTO.TableModelCollectItem item : dto.items()) {
            // 检查是否已存在（唯一性：modulePrefix + dataSource + tableName）
            SecurityTableModelTable existing = getOne(new LambdaQueryWrapper<SecurityTableModelTable>()
                    .eq(SecurityTableModelTable::getModulePrefix, item.modulePrefix())
                    .eq(SecurityTableModelTable::getDataSource, item.datasource())
                    .eq(SecurityTableModelTable::getTableName, item.tableName())
                    .eq(SecurityTableModelTable::getDeleted, false));

            if (existing != null) {
                log.info("表模型已存在，跳过采集：modulePrefix={}, datasource={}, tableName={}",
                        item.modulePrefix(), item.datasource(), item.tableName());
                continue;
            }

            // 创建表模型记录（sourceType=0 采集）
            SecurityTableModelTable tableEntity = new SecurityTableModelTable();
            tableEntity.setModulePrefix(item.modulePrefix());
            tableEntity.setDataSource(item.datasource());
            tableEntity.setTableName(item.tableName());
            tableEntity.setSourceType(0);

            // 获取服务名，从库中获取表注释
            String applicationName = getApplicationName(item.modulePrefix());
            List<TableInfo> tableInfos = tableList(applicationName, item.datasource());
            if (!CollectionUtils.isEmpty(tableInfos)) {
                tableInfos.stream()
                        .filter(ti -> Objects.equals(ti.getName(), item.tableName()))
                        .findFirst()
                        .ifPresent(ti -> tableEntity.setTableComment(ti.getRemark()));
            }

            save(tableEntity);

            // 从库中获取字段信息，批量保存
            saveColumnsFromDb(applicationName, item.datasource(), item.tableName(), tableEntity.getId());

            // 获取外键信息并保存
            saveForeignKeysFromDb(item.datasource(), item.tableName(), tableEntity.getId());
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TableModelTableVO customSave(TableModelCustomSaveDTO dto) {
        // 唯一性校验
        SecurityTableModelTable existing = getOne(new LambdaQueryWrapper<SecurityTableModelTable>()
                .eq(SecurityTableModelTable::getModulePrefix, dto.modulePrefix())
                .eq(SecurityTableModelTable::getDataSource, dto.datasource())
                .eq(SecurityTableModelTable::getTableName, dto.tableName())
                .eq(SecurityTableModelTable::getDeleted, false));

        if (existing != null) {
            throw new IllegalArgumentException("表模型已存在：modulePrefix=%s, datasource=%s, tableName=%s"
                    .formatted(dto.modulePrefix(), dto.datasource(), dto.tableName()));
        }

        // 创建表模型记录（sourceType=1 自定义添加）
        SecurityTableModelTable tableEntity = new SecurityTableModelTable();
        tableEntity.setModulePrefix(dto.modulePrefix());
        tableEntity.setDataSource(dto.datasource());
        tableEntity.setTableName(dto.tableName());
        tableEntity.setSourceType(1);

        // 从库中获取表注释
        List<TableInfo> tableInfos = tableList(dto.applicationName(), dto.datasource());
        if (!CollectionUtils.isEmpty(tableInfos)) {
            tableInfos.stream()
                    .filter(ti -> Objects.equals(ti.getName(), dto.tableName()))
                    .findFirst()
                    .ifPresent(ti -> tableEntity.setTableComment(ti.getRemark()));
        }

        save(tableEntity);

        // 从库中获取字段信息并保存
        saveColumnsFromDb(dto.applicationName(), dto.datasource(), dto.tableName(), tableEntity.getId());

        // 获取外键信息并保存
        saveForeignKeysFromDb(dto.datasource(), dto.tableName(), tableEntity.getId());

        return tableEntity.toVo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean syncTableModel(String tableModelId) {
        // 获取表记录
        SecurityTableModelTable tableEntity = super.getById(tableModelId);
        if (tableEntity == null) {
            throw new IllegalArgumentException("表模型不存在：id=" + tableModelId);
        }

        String applicationName = getApplicationName(tableEntity.getModulePrefix());

        // 从库中获取最新字段信息
        List<ColumnInfo> latestColumns = columnList(applicationName, tableEntity.getDataSource(), tableEntity.getTableName());
        if (CollectionUtils.isEmpty(latestColumns)) {
            log.warn("库中未找到表 {} 的字段信息，跳过同步", tableEntity.getTableName());
            return false;
        }

        // 获取已有字段
        List<SecurityTableModelColumn> existingColumns = securityTableModelColumnService.list(
                new LambdaQueryWrapper<SecurityTableModelColumn>()
                        .eq(SecurityTableModelColumn::getTableId, tableModelId)
                        .eq(SecurityTableModelColumn::getDeleted, false));

        // 构建已有字段名集合
        Set<String> existingColumnNames = existingColumns.stream()
                .map(SecurityTableModelColumn::getColumnName)
                .collect(Collectors.toSet());

        // 构建最新字段名集合
        Set<String> latestColumnNames = latestColumns.stream()
                .map(ColumnInfo::getName)
                .collect(Collectors.toSet());

        // 找出需要新增的字段（最新中有，已有中没有）
        List<ColumnInfo> columnsToAdd = latestColumns.stream()
                .filter(ci -> !existingColumnNames.contains(ci.getName()))
                .toList();

        // 找出需要删除的字段（已有中有，最新中没有）
        List<String> columnIdsToRemove = existingColumns.stream()
                .filter(ec -> !latestColumnNames.contains(ec.getColumnName()))
                .map(SecurityTableModelColumn::getId)
                .toList();

        // 新增字段批量保存
        if (!CollectionUtils.isEmpty(columnsToAdd)) {
            List<SecurityTableModelColumn> newColumns = columnsToAdd.stream()
                    .map(ci -> buildColumnEntity(ci, tableModelId))
                    .toList();
            securityTableModelColumnService.saveBatch(newColumns);
        }

        // 删除字段批量删除
        if (!CollectionUtils.isEmpty(columnIdsToRemove)) {
            securityTableModelColumnService.removeByIds(columnIdsToRemove);
        }

        log.info("表模型同步完成：tableModelId={}, 新增字段{}个, 删除字段{}个",
                tableModelId, columnsToAdd.size(), columnIdsToRemove.size());

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changeDatasource(TableModelChangeDatasourceDTO dto) {
        // 获取表模型记录
        SecurityTableModelTable tableEntity = super.getById(dto.tableModelId());
        if (tableEntity == null) {
            throw new IllegalArgumentException("表模型不存在：id=" + dto.tableModelId());
        }

        // 查询所有关联的 api_table_model 记录（通过 modulePrefix + tableName 匹配）
        List<SecurityApiTableModel> allRelatedApiModels = apiTableModelService.list(
                new LambdaQueryWrapper<SecurityApiTableModel>()
                        .eq(SecurityApiTableModel::getModulePrefix, tableEntity.getModulePrefix())
                        .eq(SecurityApiTableModel::getTableName, tableEntity.getTableName()));

        if (CollectionUtils.isEmpty(dto.apiIds())) {
            // 场景1：apiIds 为空 → 修改所有关联接口的数据源
            // 直接修改 tablemodel_tables 的 dataSource
            tableEntity.setDataSource(dto.newDatasource());
            updateById(tableEntity);

            // 为所有关联的 api_table_model 添加 config 记录
            for (SecurityApiTableModel apiModel : allRelatedApiModels) {
                // 先删除旧的 config
                configMapper.delete(new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                        .eq(SecurityApiTableModelConfig::getTableModelId, apiModel.getId()));
                // 再新增 config
                SecurityApiTableModelConfig config = new SecurityApiTableModelConfig();
                config.setTableModelId(apiModel.getId());
                config.setDatasource(dto.newDatasource());
                configMapper.insert(config);
            }
        } else {
            // 场景2：apiIds 不为空 → 只修改部分接口
            // 原始 tablemodel_tables 不变

            // 检查新数据源的表模型是否已存在
            SecurityTableModelTable newDatasourceTable = getOne(new LambdaQueryWrapper<SecurityTableModelTable>()
                    .eq(SecurityTableModelTable::getModulePrefix, tableEntity.getModulePrefix())
                    .eq(SecurityTableModelTable::getDataSource, dto.newDatasource())
                    .eq(SecurityTableModelTable::getTableName, tableEntity.getTableName())
                    .eq(SecurityTableModelTable::getDeleted, false));

            if (newDatasourceTable == null) {
                // 如果不存在则复制表模型数据（table + columns + foreignKeys）
                String applicationName = getApplicationName(tableEntity.getModulePrefix());

                // 复制表记录
                SecurityTableModelTable newTable = new SecurityTableModelTable();
                newTable.setModulePrefix(tableEntity.getModulePrefix());
                newTable.setDataSource(dto.newDatasource());
                newTable.setTableName(tableEntity.getTableName());
                newTable.setTableComment(tableEntity.getTableComment());
                newTable.setSourceType(tableEntity.getSourceType());
                save(newTable);

                // 从新数据源获取字段信息并保存
                saveColumnsFromDb(applicationName, dto.newDatasource(), tableEntity.getTableName(), newTable.getId());

                // 从新数据源获取外键信息并保存
                saveForeignKeysFromDb(dto.newDatasource(), tableEntity.getTableName(), newTable.getId());
            }

            // 为选中的 api_table_model 添加 config 记录
            Set<String> targetApiIdSet = new HashSet<>(dto.apiIds());
            for (SecurityApiTableModel apiModel : allRelatedApiModels) {
                if (targetApiIdSet.contains(apiModel.getApiId())) {
                    // 先删除旧的 config
                    configMapper.delete(new LambdaQueryWrapper<SecurityApiTableModelConfig>()
                            .eq(SecurityApiTableModelConfig::getTableModelId, apiModel.getId()));
                    // 再新增 config
                    SecurityApiTableModelConfig config = new SecurityApiTableModelConfig();
                    config.setTableModelId(apiModel.getId());
                    config.setDatasource(dto.newDatasource());
                    configMapper.insert(config);
                }
            }
        }

        return true;
    }

    @Override
    public Boolean updateTableComment(String tableId, String tableComment) {
        SecurityTableModelTable entity = super.getById(tableId);
        if (entity == null) {
            throw new IllegalArgumentException("表模型不存在：id=" + tableId);
        }
        entity.setTableComment(tableComment);
        return updateById(entity);
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过 providers 查找 modulePrefix 对应的 applicationName
     *
     * @param modulePrefix 模块前缀
     * @return 服务名
     */
    private String getApplicationName(String modulePrefix) {
        if (CollectionUtils.isEmpty(providers)) {
            return "";
        }
        return providers.stream()
                .filter(p -> p.module().prefix().equals(modulePrefix))
                .findFirst()
                .map(BusinessModuleInfoProvider::applicationName)
                .orElse("");
    }

    /**
     * 构建未采集表模型的 VO 对象
     */
    private TableModelTableVO buildTableVO(String modulePrefix, String datasource, String tableName) {
        TableModelTableVO vo = new TableModelTableVO();
        vo.setModulePrefix(modulePrefix);
        vo.setDataSource(datasource);
        vo.setTableName(tableName);
        vo.setSourceType(0);
        return vo;
    }

    /**
     * 从数据库获取列信息并批量保存
     *
     * @param applicationName 服务名
     * @param datasource      数据源
     * @param tableName       表名
     * @param tableId         表模型ID
     */
    private void saveColumnsFromDb(String applicationName, String datasource, String tableName, String tableId) {
        List<ColumnInfo> columnInfos = columnList(applicationName, datasource, tableName);
        if (CollectionUtils.isEmpty(columnInfos)) {
            return;
        }

        List<SecurityTableModelColumn> columns = columnInfos.stream()
                .map(ci -> buildColumnEntity(ci, tableId))
                .toList();
        securityTableModelColumnService.saveBatch(columns);
    }

    /**
     * 从 ColumnInfo 构建 SecurityTableModelColumn 实体
     */
    private SecurityTableModelColumn buildColumnEntity(ColumnInfo ci, String tableId) {
        SecurityTableModelColumn column = new SecurityTableModelColumn();
        column.setTableId(tableId);
        column.setColumnName(ci.getName());
        column.setColumnType(ci.getType());
        column.setColumnLength(ci.getLength());
        column.setColumnScale(ci.getScale());
        column.setIsNullable(ci.getNullable());
        column.setIsPrimaryKey(ci.getIsPrimaryKey());
        column.setDefaultValue(ci.getDefaultValue());
        column.setColumnComment(ci.getRemark());
        column.setOrdinalPosition(ci.getPosition());
        return column;
    }

    /**
     * 从数据库获取外键信息并批量保存
     *
     * @param datasource 数据源
     * @param tableName  表名
     * @param tableId    表模型ID
     */
    private void saveForeignKeysFromDb(String datasource, String tableName, String tableId) {
        List<ForeignKeyInfo> foreignKeyInfos;
        if (DeployUtils.isSingle()) {
            foreignKeyInfos = sqlExecutionService.foreignKeyList(datasource, tableName);
        } else {
            // 分布式模式下通过 REST 接口获取外键（暂不支持，返回空列表）
            foreignKeyInfos = Collections.emptyList();
        }

        if (CollectionUtils.isEmpty(foreignKeyInfos)) {
            return;
        }

        List<SecurityTableModelForeignKey> foreignKeys = foreignKeyInfos.stream()
                .map(fki -> buildForeignKeyEntity(fki, tableId))
                .toList();
        securityTableModelForeignKeyService.saveBatch(foreignKeys);
    }

    /**
     * 从 ForeignKeyInfo 构建 SecurityTableModelForeignKey 实体
     */
    private SecurityTableModelForeignKey buildForeignKeyEntity(ForeignKeyInfo fki, String tableId) {
        SecurityTableModelForeignKey fk = new SecurityTableModelForeignKey();
        fk.setTableId(tableId);
        fk.setConstraintName(fki.getName());
        fk.setColumnName(fki.getColumnName());
        fk.setReferencedTableName(fki.getReferencedTableName());
        fk.setReferencedColumnName(fki.getReferencedColumnName());
        fk.setDataType(0); // 采集
        // 将规则编码转为字符串
        fk.setUpdateRule(fki.getUpdateRule() != null ? String.valueOf(fki.getUpdateRule()) : null);
        fk.setDeleteRule(fki.getDeleteRule() != null ? String.valueOf(fki.getDeleteRule()) : null);
        return fk;
    }
}
