package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 统计各模块未采集表模型数量DTO
 *
 * @author Quyq
 */
@Schema(description = "统计各模块未采集表模型数量DTO")
public record TableModelUncollectedCountDTO(
        @Schema(description = "模块列表")
        List<ModuleItem> modules
) {
    @Schema(description = "模块项")
    public record ModuleItem(
            @Schema(description = "模块前缀")
            String modulePrefix,
            @Schema(description = "服务名")
            String applicationName
    ) {}
}
