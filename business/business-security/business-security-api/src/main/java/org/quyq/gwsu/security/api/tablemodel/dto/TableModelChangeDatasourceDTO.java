package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 修改表模型数据源DTO
 *
 * @author Quyq
 * @date 2026/5/18
 */
@Schema(description = "修改表模型数据源DTO")
public record TableModelChangeDatasourceDTO(
        @Schema(description = "服务名")
        String applicationName,
        @Schema(description = "表模型ID")
        String tableModelId,
        @Schema(description = "新数据源")
        String newDatasource,
        @Schema(description = "关联的接口ID列表（为空表示修改所有接口）")
        List<String> apiIds
) {}
