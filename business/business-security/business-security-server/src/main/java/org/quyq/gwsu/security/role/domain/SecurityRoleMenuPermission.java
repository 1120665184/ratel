package org.quyq.gwsu.security.role.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_role_menu_permission", autoResultMap = true)
@Schema(description = "角色菜单权限关联表")
public class SecurityRoleMenuPermission extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "角色菜单关联ID")
    private String roleMenuId;

    @Schema(description = "ABAC接口权限ID")
    private String abacPermissionId;
}
