package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

/**
 * 外键约束信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "外键约束信息")
public class TableModelForeignKeyVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "约束名称")
    private String constraintName;

    @Schema(description = "所属表ID")
    private String tableId;

    @Schema(description = "字段ID")
    private String columnId;

    @Schema(description = "引用表ID")
    private String referencedTableId;

    @Schema(description = "引用字段ID")
    private String referencedColumnId;

    @Schema(description = "更新规则")
    private String updateRule;

    @Schema(description = "删除规则")
    private String deleteRule;
}
