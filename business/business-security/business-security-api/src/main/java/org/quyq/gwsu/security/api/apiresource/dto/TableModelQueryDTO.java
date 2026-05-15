package org.quyq.gwsu.security.api.apiresource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "表模型查询条件")
public class TableModelQueryDTO extends BaseDTO {

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "表名（模糊查询）")
    private String tableName;

    @Schema(description = "接口资源ID")
    private String apiId;

    @Schema(description = "角色ID（查询角色可配置的表模型）")
    private String roleId;
}
