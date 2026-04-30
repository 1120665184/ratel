package org.quyq.gwsu.security.api.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

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
}
