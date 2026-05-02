package org.quyq.gwsu.security.api.role.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.security.api.role.enums.CycleType;
import org.quyq.gwsu.security.api.role.enums.ValidType;

import java.time.LocalDateTime;

/**
 * 角色菜单关联信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "角色菜单关联信息")
public class RoleMenuVO extends BaseVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "菜单ID")
    private String menuId;

    @Schema(description = "时效类型：1-永久 2-绝对时间范围 3-周期性")
    private ValidType validType;

    @Schema(description = "绝对时间-开始时间")
    private LocalDateTime validStart;

    @Schema(description = "绝对时间-结束时间")
    private LocalDateTime validEnd;

    @Schema(description = "周期类型：1-按周 2-按月")
    private CycleType cycleType;

    @Schema(description = "周期值：按周存1,2,3,4,5 按月存1,15")
    private String cycleValue;

    @Schema(description = "周期-每日开始时间，格式HH:mm")
    private String cycleStartTime;

    @Schema(description = "周期-每日结束时间，格式HH:mm")
    private String cycleEndTime;

}
