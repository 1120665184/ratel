package org.quyq.gwsu.common.deploy.controller;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.deploy.dto.SQLQueryDTO;
import org.quyq.gwsu.common.security.domain.vo.SqlQueryVO;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/5/16
 * @description sql执行控制器
 */
@ResponseBody
@RestController
@RequiredArgsConstructor
public class DistributedSqlExecutionController {

    private final ISQLExecutionService sqlExecutionService;


    /**
     * 执行sql
     *
     * @param dto
     * @return
     */
    @PostMapping(CoreConstants.EndPoint.ENDPOINT_DB_EXECUTION)
    public R<SqlQueryVO> query(@RequestBody SQLQueryDTO dto) {
        return R.ok(sqlExecutionService.query(dto.datasource(), dto.sql(), dto.params()));
    }

    /**
     * 获取数据源
     *
     * @return
     */
    @PostMapping(CoreConstants.EndPoint.ENDPOINT_DB_DATASOURCE)
    public R<List<String>> queryDatasource() {
        return R.ok(sqlExecutionService.datasourceList());
    }

    /**
     * 表信息获取
     *
     * @param datasource
     * @return
     */
    @GetMapping(CoreConstants.EndPoint.ENDPOINT_DB_TABLES)
    public R<?> queryTables(@RequestParam(required = false) String datasource) {
        return R.ok(sqlExecutionService.tableList(datasource));
    }

    /**
     * 获取指定表的列信息
     *
     * @param datasource
     * @param tableName
     * @return
     */
    @GetMapping(CoreConstants.EndPoint.ENDPOINT_DB_COLUMNS)
    public R<?> queryColumns(@RequestParam(required = false) String datasource, @RequestParam String tableName) {
        return R.ok(sqlExecutionService.columnList(datasource, tableName));
    }


    /**
     * 获取指定数据源的数据库名
     * @param datasource
     * @return
     */
    @GetMapping(CoreConstants.EndPoint.ENDPOINT_DB_NAME)
    public R<String> getDatabaseName(@RequestParam(required = false) String datasource) {
        return R.ok(sqlExecutionService.getDatabaseName(datasource));
    }


}
