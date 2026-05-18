package org.quyq.gwsu.security.api.tablemodel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

/**
 * 表模型分页查询条件
 *
 * @author Quyq
 * @date 2026/5/18
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "表模型分页查询条件")
public class TableModelTableQueryDTO extends BaseDTO {

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "表名（模糊查询）")
    private String tableName;

    @Schema(description = "数据源")
    private String dataSource;

    @Schema(description = "来源类型：0-采集 1-自定义添加")
    private Integer sourceType;
}
