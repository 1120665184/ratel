package org.quyq.gwsu.security.api.dataresource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 数据资源查询条件
 *
 * @author Quyq
 * @date 2026/4/20
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "数据资源查询条件")
public class DataResourceQueryDTO extends BaseDTO {

    @Schema(description = "表名（模糊查询）")
    private String tableName;

    @Schema(description = "Catalog 名称（模糊查询）")
    private String catalogName;

    @Schema(description = "数据库/Schema 名称（模糊查询）")
    private String schemaName;

    @Schema(description = "启用状态")
    private Boolean status;

}
