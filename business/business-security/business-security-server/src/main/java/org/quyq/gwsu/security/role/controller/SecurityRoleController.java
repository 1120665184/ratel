package org.quyq.gwsu.security.role.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.quyq.gwsu.common.security.api.IRoleInfoClientApi;
import org.quyq.gwsu.common.security.api.vo.UserRoleInfo;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.role.RoleClientApi;
import org.quyq.gwsu.security.api.role.dto.RoleQueryDTO;
import org.quyq.gwsu.security.api.role.dto.RoleValidGroupDTO;
import org.quyq.gwsu.security.api.role.enums.RoleType;
import org.quyq.gwsu.security.api.role.vo.EnumOptionVO;
import org.quyq.gwsu.security.api.role.vo.MenuTreeNodeVO;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.quyq.gwsu.security.api.role.vo.RoleValidGroupVO;
import org.quyq.gwsu.security.role.domain.SecurityRole;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenuPermission;
import org.quyq.gwsu.security.role.domain.SecurityRoleSubject;
import org.quyq.gwsu.security.role.service.ISecurityRoleService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("role")
@Tag(name = "角色管理", description = "角色管理接口")
@RequiredArgsConstructor
@TableModelPermission({SecurityRole.class, SecurityRoleMenu.class, SecurityRoleMenuPermission.class, SecurityRoleSubject.class})
public class SecurityRoleController implements RoleClientApi, IRoleInfoClientApi {

    private final ISecurityRoleService roleService;

    private final SecurityUtils securityUtils;

    @Operation(summary = "根据ID查询角色")
    @GetMapping("/{id}")
    @Override
    public R<RoleVO> getById(@PathVariable String id) {
        return R.ok(roleService.getById(id));
    }


    @Operation(summary = "分页查询角色")
    @PostMapping("/page")
    public R<IPage<RoleVO>> page(@RequestBody RoleQueryDTO query) {
        return R.ok(roleService.pageByCondition(query));
    }

    @Operation(summary = "获取当前登录用户的角色信息")
    @GetMapping("rolesByCurrUser")
    public R<List<RoleVO>> rolesByIdents() {
        Optional<Subject<Visitor>> subject = securityUtils.getSubject();
        return subject.map(visitorSubject -> R.ok(roleService.roleByIdents(visitorSubject.getRoles()))).orElseGet(() -> R.ok(Collections.emptyList()));
    }


    @Operation(summary = "根据主体ID查询角色标识列表")
    @GetMapping("list/{subjectId}")
    @Override
    public R<UserRoleInfo> getRoleListBySubject(@PathVariable String subjectId) {
        List<RoleVO> roles = roleService.listBySubjectId(subjectId);
        Set<String> roleCodes = roles.stream().map(RoleVO::getRoleCode)
                .collect(Collectors.toSet());
        //添加通用角色
        roleCodes.add(SecurityConstants.Authentication.ROLE_COMMON_FLAG);

        DataScope dataScope = roles.stream()
                .filter(role -> role.getDataScope() != null)   // 忽略 null 值
                .min(Comparator.comparing(RoleVO::getDataScope))
                .map(role -> DataScope.of(role.getDataScope()))
                .orElse(DataScope.SELF_ONLY);

        return R.ok(new UserRoleInfo(dataScope, new ArrayList<>(roleCodes)));
    }

    @Operation(summary = "新增或更新角色")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody RoleVO vo) {
        return R.ok(roleService.saveOrUpdateRole(SecurityRole.toDo(vo)));
    }

    @Operation(summary = "批量删除角色")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(roleService.removeByIds(ids));
    }

    @Operation(summary = "分配角色菜单")
    @PostMapping("/{roleId}/menus")
    public R<Boolean> assignMenus(@PathVariable String roleId, @RequestBody List<String> menuIds) {
        return R.ok(roleService.assignMenus(roleId, menuIds));
    }

    @Operation(summary = "启用/禁用角色")
    @PutMapping("/status")
    public R<Boolean> updateStatus(@RequestParam String id, @RequestParam Integer status) {
        return R.ok(roleService.updateStatus(id, status));
    }

    @Operation(summary = "获取角色菜单权限时效分组列表")
    @GetMapping("/valid-groups/{roleId}")
    public R<List<RoleValidGroupVO>> listValidGroups(@PathVariable String roleId) {
        return R.ok(roleService.listValidGroups(roleId));
    }

    @Operation(summary = "获取完整菜单树（含角色关联状态）")
    @GetMapping("/menu-tree")
    public R<List<MenuTreeNodeVO>> getMenuTree(@RequestParam String roleId,
                                               @RequestParam(required = false) MenuOwner owner) {
        return R.ok(roleService.getMenuTreeWithRoleBinding(roleId, owner));
    }

    @Operation(summary = "新增或更新菜单权限时效组")
    @PostMapping("/valid-group")
    public R<Boolean> saveOrUpdateValidGroup(@RequestBody RoleValidGroupDTO dto) {
        return R.ok(roleService.saveOrUpdateValidGroup(dto));
    }

    @Operation(summary = "删除菜单权限时效组")
    @DeleteMapping("/valid-group/{roleMenuId}")
    public R<Boolean> deleteValidGroup(@PathVariable String roleMenuId) {
        return R.ok(roleService.deleteValidGroup(roleMenuId));
    }

    @Operation(summary = "查询角色全量列表")
    @GetMapping("/list")
    public R<List<RoleVO>> list(@RequestParam(required = false) Integer status) {
        RoleQueryDTO query = new RoleQueryDTO();
        query.setStatus(status != null ? status != 0 : null);
        return R.ok(roleService.listByCondition(query));
    }

    @Operation(summary = "获取角色类型枚举选项")
    @GetMapping("/enums/role-type")
    public R<List<EnumOptionVO>> roleTypeOptions() {
        return R.ok(java.util.Arrays.stream(RoleType.values())
                .map(e -> new EnumOptionVO(e.getDescription(), e.getCode()))
                .toList());
    }

    @Operation(summary = "获取数据范围枚举选项")
    @GetMapping("/enums/data-scope")
    public R<List<EnumOptionVO>> dataScopeOptions() {
        return R.ok(java.util.Arrays.stream(DataScope.values())
                .map(e -> new EnumOptionVO(e.getDescription(), e.getCode()))
                .toList());
    }


    @Operation(summary = "给主体分配角色")
    @PutMapping("/allocationRole/{subjectId}")
    public R<Void> allocationRoleToSubject(@PathVariable String subjectId, @RequestBody List<String> roleIds) {
        roleService.allocationRoleToSubject(subjectId, roleIds);
        return R.ok();
    }

    @Operation(summary = "给角色分配主体")
    @PutMapping("/allocationSubject/{roleId}")
    public R<Void> allocationSubjectToRole(@PathVariable String roleId, @RequestBody List<String> subjectIds) {
        roleService.allocationSubjectToRole(roleId, subjectIds);
        return R.ok();
    }

    @Operation(summary = "根据角色ID查询关联的主体ID列表")
    @GetMapping("/{roleId}/subjects")
    public R<List<String>> listSubjectIdsByRoleId(@PathVariable String roleId) {
        return R.ok(roleService.listSubjectIdsByRoleId(roleId));
    }

}