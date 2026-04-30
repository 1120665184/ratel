package org.quyq.gwsu.security.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.IDGenerationUtils;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.api.menu.dto.MenuQueryDTO;
import org.quyq.gwsu.security.api.menu.dto.MenuSortDTO;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;
import org.quyq.gwsu.security.menu.mapper.SecurityMenuMapper;
import org.quyq.gwsu.security.menu.service.ISecurityMenuService;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityMenuServiceImpl extends ServiceImpl<SecurityMenuMapper, SecurityMenu> implements ISecurityMenuService {

    private final SecurityRoleMenuMapper roleMenuMapper;
    private final SecurityUtils securityUtils;

    private final IDGenerationUtils idGenerationUtils;

    @Override
    public MenuVO getById(String id) {
        SecurityMenu menu = super.getById(id);
        return menu != null ? menu.toVo() : null;
    }

    @Override
    public List<MenuVO> listTree(MenuQueryDTO query, MenuOwner owner) {
        LambdaQueryWrapper<SecurityMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityMenu::getDeleted, false);
        wrapper.eq(SecurityMenu::getOwner, owner);

        if (query != null) {
            if (query.getMenuName() != null && !query.getMenuName().isEmpty()) {
                wrapper.like(SecurityMenu::getMenuName, query.getMenuName());
            }
            if (query.getStatus() != null) {
                wrapper.eq(SecurityMenu::getStatus, query.getStatus());
            }
            if (query.getVisible() != null) {
                wrapper.eq(SecurityMenu::getVisible, query.getVisible());
            }
            if (query.getPosition() != null) {
                wrapper.eq(SecurityMenu::getPosition, query.getPosition());
            }
        }

        // 默认只查询目录和菜单，不返回按钮
        wrapper.in(SecurityMenu::getMenuType, 1, 2);
        wrapper.orderByAsc(SecurityMenu::getSort);

        List<SecurityMenu> menus = list(wrapper);
        return buildMenuTree(menus);
    }

    @Override
    public List<MenuVO> listTreeBySubjectId(String subjectId, MenuOwner owner) {
        // 查询用户拥有的菜单
        List<SecurityMenu> menus = baseMapper.selectMenusBySubjectId(subjectId, owner);
        return buildMenuTree(menus);
    }

    @Override
    public List<String> listMenuIdsByRoleId(String roleId, MenuOwner owner) {
        LambdaQueryWrapper<SecurityRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityRoleMenu::getRoleId, roleId);

        // 先获取该角色关联的所有菜单ID
        List<SecurityRoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        List<String> menuIds = roleMenus.stream()
                .map(SecurityRoleMenu::getMenuId)
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 再过滤出指定owner的菜单ID
        LambdaQueryWrapper<SecurityMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(SecurityMenu::getId, menuIds)
                .eq(SecurityMenu::getOwner, owner)
                .eq(SecurityMenu::getDeleted, false);
        List<SecurityMenu> menus = list(menuWrapper);

        return menus.stream()
                .map(SecurityMenu::getId)
                .collect(Collectors.toList());
    }


    @Override
    public Boolean saveOrUpdateMenu(SecurityMenu menu) {
        if (StringUtils.hasText(menu.getId())) {
            return super.updateById(menu);
        }
        menu.setId(idGenerationUtils.generateNextIdStr(3));
        return super.save(menu);
    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        // 检查是否有子菜单
        long childCount = count(new LambdaQueryWrapper<SecurityMenu>()
                .in(SecurityMenu::getParentId, ids)
                .eq(SecurityMenu::getDeleted, false));
        if (childCount > 0) {
            throw new IllegalArgumentException("存在子菜单，无法删除");
        }
        return removeBatchByIds(ids);
    }

    @Override
    public List<MenuVO> listUserRoutes(MenuOwner owner) {
        // 获取当前登录主体
        var subjectOpt = securityUtils.getSubject();
        if (subjectOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Subject<?> subject = subjectOpt.get();

        // 判断是否为超级管理员
        if (subject.isAdmin()) {
            // 返回所有启用的菜单树
            MenuQueryDTO query = new MenuQueryDTO();
            query.setStatus(true);
            return listTree(query, owner);
        }

        // 通过角色编码列表获取关联的菜单树
        List<String> roleCodes = subject.getRoles();
        return listTreeByRoleCodes(roleCodes, owner);
    }

    @Override
    public List<MenuVO> listTreeByRoleCodes(List<String> roleCodes, MenuOwner owner) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new ArrayList<>();
        }
        List<SecurityMenu> menus = baseMapper.selectMenusByRoleCodes(roleCodes, owner);
        return buildMenuTree(menus);
    }

    @Override
    public List<MenuVO> listButtonsByParentId(String parentId, MenuOwner owner) {
        LambdaQueryWrapper<SecurityMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityMenu::getDeleted, false)
                .eq(SecurityMenu::getParentId, parentId)
                .eq(SecurityMenu::getMenuType, 3)
                .eq(SecurityMenu::getOwner, owner)
                .orderByAsc(SecurityMenu::getSort);
        return list(wrapper).stream()
                .map(SecurityMenu::toVo)
                .toList();
    }

    @Override
    public Boolean batchSort(List<MenuSortDTO> sortItems) {
        for (MenuSortDTO item : sortItems) {
            SecurityMenu menu = new SecurityMenu();
            menu.setId(item.getId());
            menu.setParentId(item.getParentId());
            menu.setSort(item.getSort());
            updateById(menu);
        }
        return true;
    }

    /**
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<SecurityMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<MenuVO>> groupByParent = menus.stream()
                .map(SecurityMenu::toVo)
                .collect(Collectors.groupingBy(
                        vo -> vo.getParentId() != null ? vo.getParentId() : SecurityMenu.ROOT_MENU_PARENT_ID
                ));

        List<MenuVO> roots = groupByParent.getOrDefault(SecurityMenu.ROOT_MENU_PARENT_ID, new ArrayList<>());

        roots.forEach(root -> setChildren(root, groupByParent));

        return roots;
    }

    /**
     * 递归设置子菜单
     */
    private void setChildren(MenuVO parent, Map<String, List<MenuVO>> groupByParent) {
        List<MenuVO> children = groupByParent.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            children.forEach(child -> setChildren(child, groupByParent));
        }
    }
}
