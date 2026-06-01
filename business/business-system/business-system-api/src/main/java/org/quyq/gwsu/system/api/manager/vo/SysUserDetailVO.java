package org.quyq.gwsu.system.api.manager.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "用户详情（含账号+部门）")
public class SysUserDetailVO extends UserVO {

    @Schema(description = "账号列表")
    private List<AccountVO> accounts;

    @Schema(description = "部门关联列表")
    private List<SysUserDeptVO> depts;

    /**
     * 是否超级管理员
     */
    private boolean admin;
}
