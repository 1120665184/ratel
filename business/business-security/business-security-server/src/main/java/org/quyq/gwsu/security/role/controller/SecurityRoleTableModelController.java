package org.quyq.gwsu.security.role.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.api.role.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.role.vo.RolePermissionTableModelVO;
import org.quyq.gwsu.security.role.domain.SecurityRoleTableModel;
import org.quyq.gwsu.security.role.service.ISecurityRoleTableModelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色表模型权限控制器
 */
@RestController
@RequestMapping("roleTableModel")
@Tag(name = "角色表模型权限管理", description = "角色表模型权限管理接口")
@TableModelPermission({SecurityRoleTableModel.class})
@RequiredArgsConstructor
public class SecurityRoleTableModelController {

    private final ISecurityRoleTableModelService roleTableModelService;


    @Operation(summary = "获取指定角色的表模型权限信息")
    @GetMapping("getTableModelPermission/{roleId}")
    public R<List<RolePermissionTableModelVO>> getTableModelPermission(@PathVariable String roleId) {
        return R.ok(roleTableModelService.getTableModelPermission(roleId));
    }

    @Operation(summary = "保存或更新角色表模型权限")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody RoleTableModelSaveDTO dto) {
        return R.ok(roleTableModelService.saveOrUpdateRoleTableModel(dto));
    }

    @Operation(summary = "批量删除")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(roleTableModelService.removeByIds(ids));
    }
}
