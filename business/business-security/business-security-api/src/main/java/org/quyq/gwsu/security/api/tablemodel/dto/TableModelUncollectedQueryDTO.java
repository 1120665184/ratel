package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "查询未采集表模型请求")
public record TableModelUncollectedQueryDTO(
        @Schema(description = "模块前缀")
        String modulePrefix
) {}
