package org.quyq.gwsu.security.api.menu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.enums.MenuPosition;

import java.util.List;

/**
 * 菜单信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "菜单信息")
public class MenuVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "父菜单ID")
    private String parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型：1-目录 2-菜单 3-按钮")
    private Integer menuType;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "是否显示")
    private Boolean visible;

    @Schema(description = "状态：true-正常 false-禁用")
    private Boolean status;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "按钮标识，格式：菜单ID_标识")
    private String buttonKey;

    @Schema(description = "功能描述，用于AI提示词构建")
    private String description;

    @Schema(description = "菜单位置类型")
    private MenuPosition position;

    @Schema(description = "菜单所属类型")
    private MenuOwner owner;

    @Schema(description = "子菜单列表")
    private List<MenuVO> children;

}
