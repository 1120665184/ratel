package org.quyq.gwsu.security.api.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.enums.MenuPosition;

/**
 * 菜单查询条件
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "菜单查询条件")
public class MenuQueryDTO extends BaseDTO {

    @Schema(description = "菜单名称（模糊查询）")
    private String menuName;

    @Schema(description = "菜单类型：1-目录 2-菜单 3-按钮")
    private Integer menuType;

    @Schema(description = "状态：true-正常 false-禁用")
    private Boolean status;

    @Schema(description = "是否显示")
    private Boolean visible;

    @Schema(description = "菜单位置类型")
    private MenuPosition position;

    @Schema(description = "菜单所属类型")
    private MenuOwner owner;
}
