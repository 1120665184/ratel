package org.quyq.gwsu.security.role.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.security.api.role.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.role.vo.RoleTableModelVO;
import org.quyq.gwsu.security.role.service.ISecurityRoleTableModelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色表模型权限控制器
 */
@RestController
@RequestMapping("roleTableModel")
@Tag(name = "角色表模型权限管理", description = "角色表模型权限管理接口")
@RequiredArgsConstructor
public class SecurityRoleTableModelController {

    private final ISecurityRoleTableModelService roleTableModelService;

    @Operation(summary = "分页查询")
    @PostMapping("page")
    public R<IPage<RoleTableModelVO>> page(@RequestBody TableModelQueryDTO query) {
        return R.ok(roleTableModelService.pageByCondition(query));
    }

    @Operation(summary = "根据角色ID查询表模型权限列表")
    @GetMapping("list/by-role/{roleId}")
    public R<List<RoleTableModelVO>> listByRoleId(@PathVariable String roleId) {
        return R.ok(roleTableModelService.listByRoleId(roleId));
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
