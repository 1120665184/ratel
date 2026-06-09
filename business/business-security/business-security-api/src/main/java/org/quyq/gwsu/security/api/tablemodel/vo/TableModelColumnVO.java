package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

/**
 * 字段详细信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "字段详细信息")
public class TableModelColumnVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "关联表ID")
    private String tableId;

    @Schema(description = "字段名")
    private String columnName;

    @Schema(description = "字段类型")
    private String columnType;

    @Schema(description = "字段长度")
    private Integer columnLength;

    @Schema(description = "字段精度")
    private Integer columnScale;

    @Schema(description = "是否可空：true-是 false-否")
    private Boolean isNullable;

    @Schema(description = "是否主键：true-是 false-否")
    private Boolean isPrimaryKey;

    @Schema(description = "主键位置")
    private Integer pkPosition;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "字段注释")
    private String columnComment;

    @Schema(description = "字段顺序")
    private Integer ordinalPosition;

    @Schema(description = "字段权限配置")
    private String fieldConfig;

    @Schema(description = "字典键（绑定枚举值）")
    private String dictKey;
}
