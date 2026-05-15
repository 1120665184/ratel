package org.quyq.gwsu.security.api.tablemodel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色表模型权限信息")
public class RoleTableModelVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "模块前缀")
    private String modulePrefix;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "数据源名称")
    private String datasource;

    @Schema(description = "字段配置列表")
    private List<TableModelFieldVO> fields;
}
