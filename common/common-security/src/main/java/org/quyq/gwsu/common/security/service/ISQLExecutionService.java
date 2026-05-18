package org.quyq.gwsu.common.security.service;


import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.common.security.domain.vo.SqlQueryVO;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description
 */
public interface ISQLExecutionService {


    /**
     * 执行查询 SQL
     *
     * @param datasource
     * @param sql
     * @param parameters
     * @return
     */
    SqlQueryVO query(String datasource, String sql, List<Object> parameters);


    /**
     * 获取数据源列表
     * @return
     */
    List<String> datasourceList();

    /**
     * 获取指定数据源的表信息
     * @param datasource
     * @return
     */
    List<TableInfo> tableList(String datasource);

    /**
     * 获取列信息
     * @param datasource
     * @param tableName
     * @return
     */
    List<ColumnInfo>  columnList(String datasource, String tableName);

    /**
     * 获取指定数据源的数据库名称
     * @param datasource
     * @return
     */
    String getDatabaseName(String datasource);

}
