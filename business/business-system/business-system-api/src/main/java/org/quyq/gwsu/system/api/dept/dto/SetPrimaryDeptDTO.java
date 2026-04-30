package org.quyq.gwsu.system.api.dept.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设置主部门请求
 *
 * @author Quyq
 */
@Data
@Schema(description = "设置主部门请求")
public class SetPrimaryDeptDTO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "主部门ID")
    private String deptId;
}