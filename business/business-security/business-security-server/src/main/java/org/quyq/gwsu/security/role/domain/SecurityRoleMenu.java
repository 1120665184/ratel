package org.quyq.gwsu.security.role.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.role.enums.CycleType;
import org.quyq.gwsu.security.api.role.enums.ValidType;
import org.quyq.gwsu.security.api.role.vo.RoleMenuVO;

import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Schema(description = "周期-每日开始时间")
    private LocalTime cycleStartTime;

    @Schema(description = "周期-每日结束时间")
    private LocalTime cycleEndTime;

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
        vo.setValidType(this.validType);
        vo.setValidStart(this.validStart);
        vo.setValidEnd(this.validEnd);
        vo.setCycleType(this.cycleType);
        vo.setCycleValue(this.cycleValue);
        vo.setCycleStartTime(this.cycleStartTime);
        vo.setCycleEndTime(this.cycleEndTime);
        vo.copyBaseProperties(this);
        return vo;
    }
}
