package org.quyq.gwsu.common.security.service.impl;


import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.database.metadata.DdlFactory;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.common.database.utils.DatabaseHelper;
import org.quyq.gwsu.common.security.domain.vo.SqlQueryVO;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.quyq.gwsu.common.security.utils.DataPermissionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description
 */
@RequiredArgsConstructor
public class SQLExecutionServiceImpl implements ISQLExecutionService {

    private final JdbcTemplate jdbcTemplate;
    private final DataPermissionUtils dataPermissionUtils;
    private final DatabaseHelper databaseHelper;
    private final DdlFactory ddlFactory;

    @Override
    public SqlQueryVO query(String datasource, String sql, List<Object> parameters) {

        if (!StringUtils.hasText(sql)) {
            return null;
        }
        //添加当前用户的数据权限
        sql = dataPermissionUtils.applyDataPermission(sql);

        if (StringUtils.hasText(datasource)) {
            DynamicDataSourceContextHolder.push(datasource);
        }
        List<Map<String, @Nullable Object>> maps;
        try {
            if (Objects.isNull(parameters)) {
                maps = jdbcTemplate.queryForList(sql);
            } else {
                maps = jdbcTemplate.queryForList(sql, parameters.toArray());
            }
            return new SqlQueryVO(sql, maps);
        } finally {
            if (StringUtils.hasText(datasource)) {
                DynamicDataSourceContextHolder.clear();
            }
        }
    }

    @Override
    public List<String> datasourceList() {
        return databaseHelper.getAllDatasourceKeys();
    }

    @Override
    public List<TableInfo> tableList(String datasource) {
        if (StringUtils.hasText(datasource)) {
            DynamicDataSourceContextHolder.push(datasource);
        }
        try {
            return ddlFactory.showTables(null);
        } finally {
            if (StringUtils.hasText(datasource)) {
                DynamicDataSourceContextHolder.clear();
            }
        }
    }

    @Override
    public List<ColumnInfo> columnList(String datasource, String tableName) {
        AssertUtils.hasText(tableName, CommonErrorCode.E01002);

        if (StringUtils.hasText(datasource)) {
            DynamicDataSourceContextHolder.push(datasource);
        }
        try {
            return ddlFactory.showColumns(null, tableName);
        } finally {
            if (StringUtils.hasText(datasource)) {
                DynamicDataSourceContextHolder.clear();
            }
        }
    }

    @Override
    public String getDatabaseName(String datasource) {
        if (StringUtils.hasText(datasource)) {
            DynamicDataSourceContextHolder.push(datasource);
        }
        try {
            return databaseHelper.getCurrentDatabaseType().getName();
        } finally {
            if (StringUtils.hasText(datasource)) {
                DynamicDataSourceContextHolder.clear();
            }
        }
    }
}
