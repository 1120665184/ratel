package org.quyq.gwsu.security.role.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.role.IRoleInfoClientApi;
import org.quyq.gwsu.security.api.role.RoleClientApi;
import org.quyq.gwsu.security.api.role.dto.RoleQueryDTO;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.quyq.gwsu.security.role.domain.SecurityRole;
import org.quyq.gwsu.security.role.service.ISecurityRoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("role")
@Tag(name = "角色管理", description = "角色管理接口")
@RequiredArgsConstructor
public class SecurityRoleController implements RoleClientApi, IRoleInfoClientApi {

    private final ISecurityRoleService roleService;

    @Operation(summary = "根据ID查询角色")
    @GetMapping("/{id}")
    @Override
    public R<RoleVO> getById(@PathVariable String id) {
        return R.ok(roleService.getById(id));
    }

    @Operation(summary = "根据角色编码查询")
    @GetMapping("/code/{roleCode}")
    @Override
    public R<RoleVO> getByCode(@PathVariable String roleCode) {
        return R.ok(roleService.getByCode(roleCode));
    }

    @Operation(summary = "分页查询角色")
    @PostMapping("/page")
    public R<IPage<RoleVO>> page(@RequestBody RoleQueryDTO query) {
        return R.ok(roleService.pageByCondition(query));
    }

    @Operation(summary = "查询角色列表")
    @GetMapping("/list")
    public R<List<RoleVO>> list(RoleQueryDTO query) {
        return R.ok(roleService.listByCondition(query));
    }

    @Operation(summary = "根据主体ID查询角色列表")
    @GetMapping("/by-subject/{subjectId}")
    @Override
    public R<List<RoleVO>> listBySubjectId(@PathVariable String subjectId) {
        return R.ok(roleService.listBySubjectId(subjectId));
    }

    @Operation(summary = "根据主体ID查询角色标识列表")
    @GetMapping("list/{subjectId}")
    @Override
    public R<List<String>> getRoleListBySubject(@PathVariable String subjectId) {
        return R.ok(roleService.listBySubjectId(subjectId)
                .stream().map(RoleVO::getRoleCode)
                .toList());
    }

    @Operation(summary = "新增或更新角色")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody SecurityRole role) {
        return R.ok(roleService.saveOrUpdateRole(role));
    }

    @Operation(summary = "批量删除角色")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(roleService.removeByIds(ids));
    }

    @Operation(summary = "分配角色菜单")
    @PostMapping("/{roleId}/menus")
    @Override
    public R<Boolean> assignMenus(@PathVariable String roleId, @RequestBody List<String> menuIds) {
        return R.ok(roleService.assignMenus(roleId, menuIds));
    }

}
