package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 表模型详细信息（表+字段+外键）
 *
 * @author Quyq
 */
@Data
@Schema(description = "表模型详细信息")
public class TableModelDetailVO {

    @Schema(description = "表基本信息")
    private TableModelTableVO table;

    @Schema(description = "字段列表")
    private List<TableModelColumnVO> columns;

    @Schema(description = "外键列表")
    private List<TableModelForeignKeyVO> foreignKeys;
}
