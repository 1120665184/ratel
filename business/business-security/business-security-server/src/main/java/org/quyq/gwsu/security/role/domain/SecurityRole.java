package org.quyq.gwsu.security.role.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.role.vo.RoleVO;

/**
 * 角色表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_role", autoResultMap = true)
@Schema(description = "角色表")
public class SecurityRole extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态：true-正常 false-禁用")
    private Boolean status;

    /**
     * DO 转 VO
     *
     * @return RoleVO
     */
    public RoleVO toVo() {
        RoleVO vo = new RoleVO();
        vo.setId(this.id);
        vo.setRoleName(this.roleName);
        vo.setRoleCode(this.roleCode);
        vo.setSort(this.sort);
        vo.setDescription(this.description);
        vo.setStatus(this.status);
        vo.copyBaseProperties(this);
        return vo;
    }
}
