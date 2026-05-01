package org.quyq.gwsu.security.api.role.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.security.api.role.enums.CycleType;
import org.quyq.gwsu.security.api.role.enums.ValidType;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Schema(description = "角色时效分组信息")
public class RoleValidGroupVO {

    @Schema(description = "security_role_menu的ID")
    private String roleMenuId;

    @Schema(description = "菜单ID")
    private String menuId;

    @Schema(description = "时效类型")
    private ValidType validType;

    @Schema(description = "绝对时间-开始时间")
    private LocalDateTime validStart;

    @Schema(description = "绝对时间-结束时间")
    private LocalDateTime validEnd;

    @Schema(description = "周期类型")
    private CycleType cycleType;

    @Schema(description = "周期值")
    private String cycleValue;

    @Schema(description = "周期-每日开始时间")
    private LocalTime cycleStartTime;

    @Schema(description = "周期-每日结束时间")
    private LocalTime cycleEndTime;

    @Schema(description = "关联的菜单/按钮数量")
    private Integer menuCount;

    @Schema(description = "关联的菜单ID列表")
    private List<String> menuIds;
}
