package org.quyq.gwsu.security.api.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.security.api.role.enums.RoleType;

/**
 * 角色查询条件
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "角色查询条件")
public class RoleQueryDTO extends BaseDTO {

    @Schema(description = "角色名称（模糊查询）")
    private String roleName;

    @Schema(description = "角色编码（模糊查询）")
    private String roleCode;

    @Schema(description = "状态：true-正常 false-禁用")
    private Boolean status;

    @Schema(description = "角色类型：1-系统角色 2-业务角色")
    private RoleType roleType;

    @Schema(description = "数据范围：0-自定义 1-全部数据 2-本部门及以下 3-本部门 4-仅本人")
    private Integer dataScope;
}
