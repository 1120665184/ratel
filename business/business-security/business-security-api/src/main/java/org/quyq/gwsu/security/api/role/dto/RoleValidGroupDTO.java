package org.quyq.gwsu.security.api.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.security.api.role.enums.CycleType;
import org.quyq.gwsu.security.api.role.enums.ValidType;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Schema(description = "角色时效组保存请求")
public class RoleValidGroupDTO {

    @Schema(description = "更新时传入security_role_menu的ID，新增时为空")
    private String roleMenuId;

    @Schema(description = "角色ID")
    private String roleId;

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

    @Schema(description = "关联的菜单/按钮ID列表")
    private List<String> menuIds;
}
