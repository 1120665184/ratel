package org.quyq.gwsu.security.role.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.role.vo.RoleMenuVO;

/**
 * 角色菜单关联表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_role_menu", autoResultMap = true)
@Schema(description = "角色菜单关联表")
public class SecurityRoleMenu extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "菜单ID")
    private String menuId;

    @Schema(description = "ABAC接口权限ID，关联security_abac_permission表")
    private String abacPermissionId;

    /**
     * DO 转 VO
     *
     * @return RoleMenuVO
     */
    public RoleMenuVO toVo() {
        RoleMenuVO vo = new RoleMenuVO();
        vo.setId(this.id);
        vo.setRoleId(this.roleId);
        vo.setMenuId(this.menuId);
        vo.setAbacPermissionId(this.abacPermissionId);
        vo.copyBaseProperties(this);
        return vo;
    }
}
