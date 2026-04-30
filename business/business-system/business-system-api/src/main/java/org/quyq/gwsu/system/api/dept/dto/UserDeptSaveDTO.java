package org.quyq.gwsu.system.api.dept.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 设置用户部门请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "设置用户部门请求")
public class UserDeptSaveDTO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "部门ID列表")
    private List<String> deptIds;

    @Schema(description = "主部门ID")
    private String primaryDeptId;
}