package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "表模型字段信息")
public class TableModelFieldVO {

    @Schema(description = "字段名")
    private String fieldName;

    @Schema(description = "是否允许查询")
    private Boolean show;

    @Schema(description = "是否脱敏")
    private Boolean desensitize;

    @Schema(description = "脱敏策略")
    private String strategy;

    @Schema(description = "自定义脱敏-不脱敏前缀长度")
    private Integer prefixNoMaskLen;

    @Schema(description = "自定义脱敏-不脱敏后缀长度")
    private Integer suffixNoMaskLen;

    @Schema(description = "自定义脱敏-脱敏标识符")
    private String symbol;

    @Schema(description = "是否为注解硬约束（不可修改）")
    private Boolean annotationLocked;
}
