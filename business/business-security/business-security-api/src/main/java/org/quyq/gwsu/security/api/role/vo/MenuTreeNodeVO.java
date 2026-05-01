package org.quyq.gwsu.security.api.role.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.enums.MenuPosition;

import java.util.List;

@Data
@Schema(description = "菜单树节点（含角色关联状态）")
public class MenuTreeNodeVO {

    @Schema(description = "菜单ID")
    private String id;

    @Schema(description = "父菜单ID")
    private String parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型：1-目录 2-菜单 3-按钮")
    private Integer menuType;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "菜单位置类型")
    private MenuPosition position;

    @Schema(description = "菜单所属类型")
    private MenuOwner owner;

    @Schema(description = "是否已被其他时效组关联（菜单互斥）")
    private Boolean disabled;

    @Schema(description = "已关联的时效组ID")
    private String boundRoleMenuId;

    @Schema(description = "子节点")
    private List<MenuTreeNodeVO> children;
}
