package org.quyq.gwsu.system.api.manager.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户部门关联信息")
public class SysUserDeptVO extends BaseVO {

    @Schema(description = "关联ID")
    private String id;

    @Schema(description = "部门ID")
    private String deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "是否主部门")
    private Boolean isPrimary;
}
