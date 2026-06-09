package org.quyq.gwsu.security.role.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.role.vo.RoleTableModelVO;

import java.util.Map;

/**
 * 角色表模型权限配置表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_role_table_model", autoResultMap = true)
@Schema(description = "角色表模型权限配置表")
public class SecurityRoleTableModel extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
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

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "字段限制配置，key为字段名（下划线格式）")
    private Map<String, FieldPermission> fieldConfig;

    @Schema(description = "是否启用：0-禁用 1-启用，仅对接口关联的表模型有效")
    private Boolean enabled;

    public RoleTableModelVO toVo() {
        RoleTableModelVO vo = new RoleTableModelVO();
        vo.setId(this.id);
        vo.setRoleId(this.roleId);
        vo.setModulePrefix(this.modulePrefix);
        vo.setTableName(this.tableName);
        vo.setDatasource(this.datasource);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static SecurityRoleTableModel toDo(RoleTableModelVO vo) {
        SecurityRoleTableModel entity = new SecurityRoleTableModel();
        entity.setId(vo.getId());
        entity.setRoleId(vo.getRoleId());
        entity.setModulePrefix(vo.getModulePrefix());
        entity.setTableName(vo.getTableName());
        entity.setDatasource(vo.getDatasource());
        return entity;
    }
}
