package org.quyq.gwsu.security.api.role.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.quyq.gwsu.security.api.role.dto.RoleTableModelSaveDTO;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/5/19
 * @description
 */
@Data
@Accessors(chain = true)
public class RolePermissionTableModelVO {

    @Schema(description = "类型：0-接口关联；1-角色自定义配置")
    private int type = 0;

    @Schema(description = "表模型ID")
    private String tableModelId;

    @Schema(description = "security_role_table_model的主键，只有类型是自定义配置时有")
    private String id;

    @Schema(description = "所属服务（模块）")
    private String modulePrefix;

    @Schema(description = "数据源")
    private String datasource;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "表注释")
    private String tableComment;

    private List<ColumnInfo> columns = List.of();



    @Data
    @Accessors(chain = true)
    public static class ColumnInfo {
        @Schema(description = "列名")
        private String columnName;

        @Schema(description = "列注释")
        private String columnComment;

        @Schema(description = "不可变的字段配置 ， 基于注解采集的，前端根据该配置禁用字段配置功能")
        private RoleTableModelSaveDTO.FieldConfigItem fixedFieldConfig;

        @Schema(description = "基于角色自定配置的自定义权限")
        private RoleTableModelSaveDTO.FieldConfigItem customFieldConfig;

    }


}
