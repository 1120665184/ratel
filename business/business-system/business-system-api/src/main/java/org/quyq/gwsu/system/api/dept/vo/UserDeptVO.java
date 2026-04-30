package org.quyq.gwsu.system.api.dept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户部门关联 VO
 *
 * @author Quyq
 */
@Data
@Schema(description = "用户部门关联")
public class UserDeptVO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "部门ID")
    private String deptId;

    @Schema(description = "是否主部门")
    private Boolean isPrimary;
}