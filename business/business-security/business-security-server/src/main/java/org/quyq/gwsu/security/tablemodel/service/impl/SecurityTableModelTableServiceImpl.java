package org.quyq.gwsu.security.tablemodel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.mapper.SecurityTableModelTableMapper;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelColumnService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelForeignKeyService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

/**
 * 表基本信息 服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityTableModelTableServiceImpl extends ServiceImpl<SecurityTableModelTableMapper, SecurityTableModelTable>
        implements ISecurityTableModelTableService {


    private final ISecurityTableModelColumnService securityTableModelColumnService;

    private final ISecurityTableModelForeignKeyService securityTableModelForeignKeyService;

    private final RestClient.Builder clientBuilder;

    private final ISQLExecutionService sqlExecutionService;

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
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SecurityTableModelColumn>()
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
        if(StringUtils.isNotBlank(datasource)){
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

        if(DeployUtils.isSingle()){
            return sqlExecutionService.columnList(datasource, tableName);
        }

        RestClient restClient = clientBuilder.clone()
                .baseUrl("http://%s".formatted(applicationName))
                .build();

        String uri = CoreConstants.EndPoint.ENDPOINT_DB_COLUMNS + "?tableName=%s".formatted(tableName);
        if(StringUtils.isNotBlank(datasource)){
            uri += "&datasource=%s".formatted(datasource);
        }
        return FeignUtils.data(restClient
                .get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }));
    }
}
