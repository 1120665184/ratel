package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 表模型采集请求DTO
 *
 * @author Quyq
 * @date 2026/5/18
 */
@Schema(description = "表模型采集请求DTO")
public record TableModelCollectDTO(
        @Schema(description = "采集项列表")
        List<TableModelCollectItem> items
) {
    @Schema(description = "采集项")
    public record TableModelCollectItem(
            @Schema(description = "服务名")
            String applicationName,
            @Schema(description = "模块前缀")
            String modulePrefix,
            @Schema(description = "数据源")
            String datasource,
            @Schema(description = "表名")
            String tableName,
            @Schema(description = "模块字段配置JSON，格式为 Map<columnName, FieldPermission>")
            String moduleFieldConfig
    ) {}
}
