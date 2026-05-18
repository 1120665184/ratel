package org.quyq.gwsu.security.tablemodel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.api.tablemodel.dto.QueryColumnsDTO;
import org.quyq.gwsu.security.api.tablemodel.dto.QueryTableDTO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 表模型管理控制器
 *
 * @author Quyq
 */
@Tag(name = "表模型管理")
@RestController
@RequestMapping("tablemodel")
@TableModelPermission({SecurityTableModelTable.class, SecurityTableModelColumn.class, SecurityTableModelForeignKey.class})
@RequiredArgsConstructor
public class SecurityTableModelTableController {

    private final ISecurityTableModelTableService securityTableModelTableService;

    @Operation(summary = "查询指定服务中指定数据源的表列表")
    @PostMapping("table/info")
    public R<List<TableInfo>> queryTableInfo(@RequestBody QueryTableDTO tableDTO) {
        AssertUtils.hasText(tableDTO.applicationName(), SecurityErrorCode.E04001);
        return R.ok(securityTableModelTableService.tableList(tableDTO.applicationName(), tableDTO.datasource()));
    }

    @Operation(summary = "查询指定服务中指定数据源,指定表的列信息")
    @PostMapping("columns/info")
    public R<List<ColumnInfo>> queryColumnsInfo(@RequestBody QueryColumnsDTO columnsDTO) {
        AssertUtils.hasText(columnsDTO.applicationName(), SecurityErrorCode.E04001);
        AssertUtils.hasText(columnsDTO.tableName(), SecurityErrorCode.E04002);
        return R.ok(securityTableModelTableService.columnList(columnsDTO.applicationName(), columnsDTO.datasource(), columnsDTO.tableName()));
    }

}
