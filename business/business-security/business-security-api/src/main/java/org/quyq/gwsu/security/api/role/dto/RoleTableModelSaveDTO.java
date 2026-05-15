package org.quyq.gwsu.security.api.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色表模型权限保存")
public class RoleTableModelSaveDTO {

    @Schema(description = "主键ID（更新时传入）")
    private String id;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "字段限制配置列表（只存储与默认值不同的限制性配置）")
    private List<FieldConfigItem> fields;

    @Data
    @Schema(description = "字段配置项")
    public static class FieldConfigItem {
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
    }
}
