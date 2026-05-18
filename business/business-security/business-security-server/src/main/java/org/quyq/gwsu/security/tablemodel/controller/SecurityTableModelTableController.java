package org.quyq.gwsu.security.tablemodel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.database.metadata.model.ColumnInfo;
import org.quyq.gwsu.common.database.metadata.model.TableInfo;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.api.tablemodel.dto.*;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelForeignKeyVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelColumn;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelForeignKey;
import org.quyq.gwsu.security.tablemodel.domain.SecurityTableModelTable;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelColumnService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelForeignKeyService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

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

    private final ISecurityTableModelColumnService securityTableModelColumnService;

    private final ISecurityTableModelForeignKeyService securityTableModelForeignKeyService;

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

    @Operation(summary = "分页查询表模型列表")
    @PostMapping("page")
    public R<IPage<TableModelTableVO>> page(@RequestBody TableModelTableQueryDTO query) {
        return R.ok(securityTableModelTableService.pageByCondition(query));
    }

    @Operation(summary = "查询未采集的表模型列表")
    @PostMapping("listUncollected")
    public R<List<TableModelTableVO>> listUncollected(@RequestBody TableModelUncollectedQueryDTO query) {
        return R.ok(securityTableModelTableService.listUncollected(query.modulePrefix()));
    }

    @Operation(summary = "采集表模型")
    @PostMapping("collect")
    public R<Boolean> collect(@RequestBody TableModelCollectDTO dto) {
        return R.ok(securityTableModelTableService.collectTableModels(dto));
    }

    @Operation(summary = "自定义添加表模型")
    @PostMapping("customSave")
    public R<TableModelTableVO> customSave(@RequestBody TableModelCustomSaveDTO dto) {
        return R.ok(securityTableModelTableService.customSave(dto));
    }

    @Operation(summary = "同步表模型字段")
    @PostMapping("sync/{tableModelId}")
    public R<Boolean> sync(@PathVariable String tableModelId) {
        return R.ok(securityTableModelTableService.syncTableModel(tableModelId));
    }

    @Operation(summary = "修改数据源")
    @PostMapping("changeDatasource")
    public R<Boolean> changeDatasource(@RequestBody TableModelChangeDatasourceDTO dto) {
        return R.ok(securityTableModelTableService.changeDatasource(dto));
    }

    @Operation(summary = "获取表模型详情")
    @GetMapping("detail")
    public R<TableModelDetailVO> detail(@RequestParam String modulePrefix,
                                         @RequestParam String datasource,
                                         @RequestParam String tableName) {
        return R.ok(securityTableModelTableService.getTableDetail(modulePrefix, datasource, tableName));
    }

    @Operation(summary = "更新字段注释")
    @PostMapping("column/updateComment")
    public R<Boolean> updateColumnComment(@RequestBody java.util.Map<String, String> params) {
        return R.ok(securityTableModelColumnService.updateComment(params.get("columnId"), params.get("columnComment")));
    }

    @Operation(summary = "更新外键备注")
    @PostMapping("foreignKey/updateRemark")
    public R<Boolean> updateForeignKeyRemark(@RequestBody java.util.Map<String, String> params) {
        return R.ok(securityTableModelForeignKeyService.updateRemark(params.get("fkId"), params.get("remark")));
    }

    @Operation(summary = "保存外键（新增/更新）")
    @PostMapping("foreignKey/save")
    public R<Boolean> saveForeignKey(@RequestBody TableModelForeignKeyVO vo) {
        return R.ok(securityTableModelForeignKeyService.saveOrUpdateForeignKey(vo));
    }

    @Operation(summary = "更新表注释")
    @PostMapping("updateTableComment")
    public R<Boolean> updateTableComment(@RequestBody java.util.Map<String, String> params) {
        return R.ok(securityTableModelTableService.updateTableComment(params.get("tableId"), params.get("tableComment")));
    }

}
