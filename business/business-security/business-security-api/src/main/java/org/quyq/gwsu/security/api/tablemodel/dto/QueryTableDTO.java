package org.quyq.gwsu.security.api.tablemodel.dto;


import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Quyq
 * @date 2026/5/18
 * @description
 */
@Schema(description = "表信息查询DTO")
public record QueryTableDTO(
        @Schema(description = "服务名")
        String applicationName ,
        @Schema(description = "数据源")
        String datasource
) {
}
