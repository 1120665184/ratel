package org.quyq.gwsu.security.role.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.quyq.gwsu.security.api.role.enums.RoleType;
import org.quyq.gwsu.security.api.role.vo.RoleVO;

import java.util.Optional;

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

    @Schema(description = "角色类型：1-系统角色 2-业务角色")
    private RoleType roleType;

    @Schema(description = "数据范围：0-自定义 1-全部数据 2-本部门及以下 3-本部门 4-仅本人")
    private DataScope dataScope;

    @Schema(description = "状态：true-正常 false-禁用")
    private Boolean status;

    /**
     * VO 转 DO
     *
     * @param vo 角色VO
     * @return SecurityRole
     */
    public static SecurityRole toDo(RoleVO vo) {
        SecurityRole entity = new SecurityRole();
        entity.setId(vo.getId());
        entity.setRoleName(vo.getRoleName());
        entity.setRoleCode(vo.getRoleCode());
        entity.setSort(vo.getSort());
        entity.setDescription(vo.getDescription());
        entity.setRoleType(vo.getRoleType());
        entity.setDataScope(Optional.ofNullable(vo.getDataScope()).map(DataScope::of).orElse(null));
        entity.setStatus(vo.getStatus());
        return entity;
    }

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
        vo.setRoleType(this.roleType);
        vo.setDataScope(Optional.ofNullable(this.dataScope).map(DataScope::getCode).orElse(null));
        vo.setStatus(this.status);
        vo.copyBaseProperties(this);
        return vo;
    }
}
