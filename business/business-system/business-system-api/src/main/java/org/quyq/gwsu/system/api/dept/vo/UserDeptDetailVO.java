package org.quyq.gwsu.system.api.dept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.system.api.dept.enums.DeptTypeEnum;

/**
 * 用户部门详情 VO（包含部门信息和用户基本信息）
 *
 * @author Quyq
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户部门详情")
public class UserDeptDetailVO extends BaseVO {

    @Schema(description = "关联ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "部门ID")
    private String deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门类型")
    private DeptTypeEnum deptType;

    @Schema(description = "是否主部门")
    private Boolean isPrimary;
}