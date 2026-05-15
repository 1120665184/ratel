package org.quyq.gwsu.security.tablemodel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelColumnVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelForeignKeyVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelColumnService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelForeignKeyService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final ISecurityTableModelTableService tableModelTableService;

    private final ISecurityTableModelColumnService tableModelColumnService;

    private final ISecurityTableModelForeignKeyService tableModelForeignKeyService;

    @Operation(summary = "根据表名列表查询表详细信息（含字段和外键）")
    @PostMapping("/detail")
    public R<List<TableModelDetailVO>> listDetailByTableNames(@RequestBody List<String> tableNames) {
        AssertUtils.isTrue(tableNames != null && !tableNames.isEmpty(), SecurityErrorCode.E03001);

        // 查询所有匹配的表
        List<SecurityTableModelTable> tables = tableModelTableService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SecurityTableModelTable>()
                        .in(SecurityTableModelTable::getTableName, tableNames)
                        .eq(SecurityTableModelTable::getDeleted, false));

        if (tables.isEmpty()) {
            return R.ok(new ArrayList<>());
        }

        // 表ID -> 表信息 映射
        Map<String, SecurityTableModelTable> tableMap = tables.stream()
                .collect(Collectors.toMap(SecurityTableModelTable::getId, t -> t));

        List<String> tableIds = new ArrayList<>(tableMap.keySet());

        // 批量查询字段
        List<SecurityTableModelColumn> columns = tableModelColumnService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SecurityTableModelColumn>()
                        .in(SecurityTableModelColumn::getTableId, tableIds)
                        .eq(SecurityTableModelColumn::getDeleted, false)
                        .orderByAsc(SecurityTableModelColumn::getOrdinalPosition));

        // 按表ID分组
        Map<String, List<SecurityTableModelColumn>> columnMap = columns.stream()
                .collect(Collectors.groupingBy(SecurityTableModelColumn::getTableId));

        // 批量查询外键
        List<SecurityTableModelForeignKey> foreignKeys = tableModelForeignKeyService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SecurityTableModelForeignKey>()
                        .in(SecurityTableModelForeignKey::getTableId, tableIds)
                        .eq(SecurityTableModelForeignKey::getDeleted, false));

        // 按表ID分组
        Map<String, List<SecurityTableModelForeignKey>> fkMap = foreignKeys.stream()
                .collect(Collectors.groupingBy(SecurityTableModelForeignKey::getTableId));

        // 组装结果
        List<TableModelDetailVO> result = tables.stream().map(table -> {
            TableModelDetailVO detail = new TableModelDetailVO();
            detail.setTable(table.toVo());

            List<TableModelColumnVO> columnVOs = columnMap.getOrDefault(table.getId(), new ArrayList<>())
                    .stream().map(SecurityTableModelColumn::toVo).toList();
            detail.setColumns(columnVOs);

            List<TableModelForeignKeyVO> fkVOs = fkMap.getOrDefault(table.getId(), new ArrayList<>())
                    .stream().map(SecurityTableModelForeignKey::toVo).toList();
            detail.setForeignKeys(fkVOs);

            return detail;
        }).toList();

        return R.ok(result);
    }
}
