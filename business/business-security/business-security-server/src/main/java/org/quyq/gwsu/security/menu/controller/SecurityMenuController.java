package org.quyq.gwsu.security.menu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.security.api.menu.MenuClientApi;
import org.quyq.gwsu.security.api.menu.dto.MenuQueryDTO;
import org.quyq.gwsu.security.api.menu.dto.MenuSortDTO;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.enums.MenuPosition;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;
import org.quyq.gwsu.security.menu.service.ISecurityMenuService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("menu")
@Tag(name = "菜单管理", description = "菜单管理接口")
@TableModelPermission({SecurityMenu.class})
@RequiredArgsConstructor
public class SecurityMenuController implements MenuClientApi {

    private final ISecurityMenuService menuService;

    @Operation(summary = "根据ID查询菜单")
    @GetMapping("/{id}")
    @Override
    public R<MenuVO> getById(@PathVariable String id) {
        return R.ok(menuService.getById(id));
    }

    @Operation(summary = "查询菜单树")
    @PostMapping("/tree")
    @Override
    public R<List<MenuVO>> listTree(@RequestBody MenuQueryDTO query) {
        MenuOwner menuOwner = AssertUtils.notNull(query.getOwner(), SecurityErrorCode.E01002);
        return R.ok(menuService.listTree(query, menuOwner , false));
    }

    @Operation(summary = "根据用户ID查询菜单树")
    @GetMapping("/tree/{owner}/by-subject/{subjectId}")
    @Override
    public R<List<MenuVO>> listTreeBySubjectId(
            @PathVariable Integer owner,
            @PathVariable String subjectId) {
        MenuOwner menuOwner = AssertUtils.notNull(MenuOwner.of(owner), SecurityErrorCode.E01002);
        return R.ok(menuService.listTreeBySubjectId(subjectId, menuOwner));
    }

    @Operation(summary = "根据角色ID查询菜单ID列表")
    @GetMapping("/ids/{owner}/by-role/{roleId}")
    public R<List<String>> listMenuIdsByRoleId(
            @PathVariable Integer owner,
            @PathVariable String roleId) {
        MenuOwner menuOwner = AssertUtils.notNull(MenuOwner.of(owner), SecurityErrorCode.E01002);
        return R.ok(menuService.listMenuIdsByRoleId(roleId, menuOwner));
    }

    @Operation(summary = "新增或更新菜单")
    @PostMapping
    public R<Boolean> saveOrUpdate(@RequestBody SecurityMenu menu) {
        AssertUtils.hasText(menu.getMenuName(), SecurityErrorCode.E01003);
        AssertUtils.hasText(menu.getDescription(), SecurityErrorCode.E01005);
        if (menu.getMenuType() != null && menu.getMenuType() == 3) {
            AssertUtils.hasText(menu.getButtonKey(), SecurityErrorCode.E01004);
        }
        return R.ok(menuService.saveOrUpdateMenu(menu));
    }

    @Operation(summary = "批量删除菜单")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(menuService.removeByIds(ids));
    }

    @Operation(summary = "获取当前用户路由菜单")
    @LoginAllowAccess
    @TableModelPermission
    @GetMapping("/routes/{owner}")
    public R<List<MenuVO>> listUserRoutes(@PathVariable Integer owner) {
        MenuOwner menuOwner = AssertUtils.notNull(MenuOwner.of(owner), SecurityErrorCode.E01002);
        return R.ok(menuService.listUserRoutes(menuOwner));
    }

    @Operation(summary = "获取菜单所属类型枚举")
    @GetMapping("/enums/owners")
    public R<List<Map<String, Object>>> listOwners() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MenuOwner owner : MenuOwner.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", owner.getCode());
            item.put("description", owner.getDescription());
            result.add(item);
        }
        return R.ok(result);
    }

    @Operation(summary = "获取菜单位置类型枚举")
    @GetMapping("/enums/positions")
    public R<List<Map<String, Object>>> listPositions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MenuPosition position : MenuPosition.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", position.getCode());
            item.put("description", position.getDescription());
            result.add(item);
        }
        return R.ok(result);
    }

    @Operation(summary = "获取指定菜单下的按钮列表")
    @GetMapping("/tree/{owner}/buttons/{menuId}")
    public R<List<MenuVO>> listButtonsByMenuId(
            @PathVariable Integer owner,
            @PathVariable String menuId) {
        MenuOwner menuOwner = AssertUtils.notNull(MenuOwner.of(owner), SecurityErrorCode.E01002);
        return R.ok(menuService.listButtonsByParentId(menuId, menuOwner));
    }

    @Operation(summary = "批量更新菜单排序和父级")
    @PutMapping("/sort")
    @Override
    public R<Boolean> batchSort(@RequestBody List<MenuSortDTO> sortItems) {
        return R.ok(menuService.batchSort(sortItems));
    }
}
