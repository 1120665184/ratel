package org.quyq.gwsu.system.api.dept.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 移除用户部门请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "移除用户部门请求")
public class RemoveUserDeptDTO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "要移除的部门ID列表")
    private List<String> deptIds;

    @Schema(description = "新主部门ID（移除主部门时必填）")
    private String newPrimaryDeptId;
}