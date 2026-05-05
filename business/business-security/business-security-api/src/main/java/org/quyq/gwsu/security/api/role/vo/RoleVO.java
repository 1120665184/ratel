package org.quyq.gwsu.security.api.role.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.security.api.role.enums.RoleType;

/**
 * 角色信息
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "角色信息")
public class RoleVO extends BaseVO {

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

    @Schema(description = "角色类型")
    private RoleType roleType;

    @Schema(description = "数据范围")
    private Integer dataScope;

    @Schema(description = "状态：true-正常 false-禁用")
    private Boolean status;

}
