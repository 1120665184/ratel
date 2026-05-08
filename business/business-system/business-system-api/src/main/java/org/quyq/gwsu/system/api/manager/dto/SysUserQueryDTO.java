package org.quyq.gwsu.system.api.manager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "用户查询条件")
public class SysUserQueryDTO extends BaseDTO {

    @Schema(description = "关键词（用户名/昵称/手机模糊搜索）")
    private String keyword;

    @Schema(description = "状态：0-禁用 1-正常")
    private Integer status;

    @Schema(description = "部门ID（查询该部门及下级部门的用户）")
    private String deptId;

    @Schema(description = "用户列表")
    private List<String> userIds;
}
