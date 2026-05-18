package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 自定义添加表模型DTO
 *
 * @author Quyq
 * @date 2026/5/18
 */
@Schema(description = "自定义添加表模型DTO")
public record TableModelCustomSaveDTO(
        @Schema(description = "服务名")
        String applicationName,
        @Schema(description = "模块前缀")
        String modulePrefix,
        @Schema(description = "数据源")
        String datasource,
        @Schema(description = "表名")
        String tableName
) {}
