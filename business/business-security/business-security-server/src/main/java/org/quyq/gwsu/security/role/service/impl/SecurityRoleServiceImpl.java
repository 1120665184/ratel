package org.quyq.gwsu.security.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.loading.RoleBindingMenuAbacLoading;
import org.quyq.gwsu.security.abac.service.PermissionAlterationManager;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.role.dto.RoleQueryDTO;
import org.quyq.gwsu.security.api.role.dto.RoleValidGroupDTO;
import org.quyq.gwsu.security.api.role.enums.RoleType;
import org.quyq.gwsu.security.api.role.enums.ValidType;
import org.quyq.gwsu.security.api.role.vo.MenuTreeNodeVO;
import org.quyq.gwsu.security.api.role.vo.RoleValidGroupVO;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;
import org.quyq.gwsu.security.menu.mapper.SecurityMenuMapper;
import org.quyq.gwsu.security.role.domain.SecurityRole;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenuPermission;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuPermissionMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMapper;
import org.quyq.gwsu.security.role.service.ISecurityRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityRoleServiceImpl extends ServiceImpl<SecurityRoleMapper, SecurityRole> implements ISecurityRoleService {

    private final SecurityRoleMenuMapper roleMenuMapper;

    private final SecurityRoleMenuPermissionMapper roleMenuPermissionMapper;

    private final SecurityMenuMapper menuMapper;

    private final PermissionAlterationManager permissionAlterationManager;

    @Override
    public RoleVO getById(String id) {
        SecurityRole role = super.getById(id);
        return role != null ? role.toVo() : null;
    }

    @Override
    public RoleVO getByCode(String roleCode) {
        SecurityRole role = getOne(new LambdaQueryWrapper<SecurityRole>()
                .eq(SecurityRole::getRoleCode, roleCode)
                .eq(SecurityRole::getDeleted, false));
        return role != null ? role.toVo() : null;
    }

    @Override
    public IPage<RoleVO> pageByCondition(RoleQueryDTO query) {
        LambdaQueryWrapper<SecurityRole> wrapper = buildQueryWrapper(query);
        wrapper.orderByAsc(SecurityRole::getSort);

        Page<SecurityRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SecurityRole> rolePage = page(page, wrapper);

        return rolePage.convert(SecurityRole::toVo);
    }

    @Override
    public List<RoleVO> listByCondition(RoleQueryDTO query) {
        LambdaQueryWrapper<SecurityRole> wrapper = buildQueryWrapper(query);
        wrapper.orderByAsc(SecurityRole::getSort);

        return list(wrapper).stream()
                .map(SecurityRole::toVo)
                .toList();
    }

    @Override
    public List<RoleVO> listBySubjectId(String subjectId) {
        List<SecurityRole> roles = baseMapper.selectRolesBySubjectId(subjectId);
        return roles.stream()
                .map(SecurityRole::toVo)
                .toList();
    }

    @Override
    public Boolean saveOrUpdateRole(SecurityRole role) {
        // 新增时校验角色编码唯一性
        if (role.getId() == null || role.getId().isEmpty()) {
            SecurityRole existing = getOne(new LambdaQueryWrapper<SecurityRole>()
                    .eq(SecurityRole::getRoleCode, role.getRoleCode())
                    .eq(SecurityRole::getDeleted, false));
            if (existing != null) {
                throw new BusinessException(SecurityErrorCode.E02002);
            }
        }
        return saveOrUpdate(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeByIds(List<String> ids) {
        // 检查是否包含系统角色
        List<SecurityRole> roles = listByIds(ids);
        for (SecurityRole role : roles) {
            if (role.getRoleType() == RoleType.SYSTEM) {
                throw new BusinessException(SecurityErrorCode.E02003);
            }
        }

        // 删除角色菜单权限关联
        List<SecurityRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .in(SecurityRoleMenu::getRoleId, ids));
        List<String> roleMenuIds = roleMenus.stream().map(SecurityRoleMenu::getId).toList();
        if (!roleMenuIds.isEmpty()) {
            roleMenuPermissionMapper.delete(new LambdaQueryWrapper<SecurityRoleMenuPermission>()
                    .in(SecurityRoleMenuPermission::getRoleMenuId, roleMenuIds));
        }

        // 删除角色菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SecurityRoleMenu>()
                .in(SecurityRoleMenu::getRoleId, ids));

        // 删除角色
        return removeBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignMenus(String roleId, List<String> menuIds) {
        SecurityRole role = baseMapper.selectById(roleId);
        if (Objects.isNull(role)) {
            throw new BusinessException(SecurityErrorCode.E02001);
        }

        ExpressionContext context = new ExpressionContext();
        context.setValue(role.getRoleCode());
        context.putExtraParam(RoleBindingMenuAbacLoading.ROLE_INFO_KEY, role);

        List<SecurityMenu> menus = Collections.emptyList();
        if (!CollectionUtils.isEmpty(menuIds)) {
            menus = menuMapper.selectByIds(menuIds);
        }
        context.putExtraParam(RoleBindingMenuAbacLoading.MENUS_INFO_KEY, menus);

        // 处理权限
        permissionAlterationManager.alterationUrlPermission(AbacPerType.ROLE_BINDING_MENU, context);

        return true;
    }

    @Override
    public Boolean updateStatus(String id, Integer status) {
        SecurityRole role = super.getById(id);
        if (role == null) {
            throw new BusinessException(SecurityErrorCode.E02001);
        }
        role.setStatus(status != 0);
        return updateById(role);
    }

    @Override
    public List<RoleValidGroupVO> listValidGroups(String roleId) {
        List<SecurityRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getRoleId, roleId));

        if (CollectionUtils.isEmpty(roleMenus)) {
            return Collections.emptyList();
        }

        // 按相同时效配置值分组
        java.util.Map<String, List<SecurityRoleMenu>> grouped = roleMenus.stream()
                .collect(Collectors.groupingBy(this::buildValidGroupKey));

        return grouped.entrySet().stream().map(entry -> {
            List<SecurityRoleMenu> groupMenus = entry.getValue();
            SecurityRoleMenu first = groupMenus.get(0);

            RoleValidGroupVO vo = new RoleValidGroupVO();
            vo.setRoleMenuId(first.getId());
            vo.setMenuId(first.getMenuId());
            vo.setValidType(first.getValidType());
            vo.setValidStart(first.getValidStart());
            vo.setValidEnd(first.getValidEnd());
            vo.setCycleType(first.getCycleType());
            vo.setCycleValue(first.getCycleValue());
            vo.setCycleStartTime(first.getCycleStartTime());
            vo.setCycleEndTime(first.getCycleEndTime());
            vo.setMenuCount(groupMenus.size());
            vo.setMenuIds(groupMenus.stream().map(SecurityRoleMenu::getMenuId).toList());
            return vo;
        }).toList();
    }

    @Override
    public List<MenuTreeNodeVO> getMenuTreeWithRoleBinding(String roleId, MenuOwner owner) {
        // 查询所有菜单
        LambdaQueryWrapper<SecurityMenu> wrapper = new LambdaQueryWrapper<SecurityMenu>()
                .eq(SecurityMenu::getDeleted, false)
                .eq(SecurityMenu::getStatus, true)
                .orderByAsc(SecurityMenu::getSort);
        if (owner != null) {
            wrapper.eq(SecurityMenu::getOwner, owner);
        }
        List<SecurityMenu> allMenus = menuMapper.selectList(wrapper);

        // 查询角色已关联的菜单
        List<SecurityRoleMenu> boundMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getRoleId, roleId));

        java.util.Map<String, SecurityRoleMenu> boundMenuMap = boundMenus.stream()
                .collect(Collectors.toMap(SecurityRoleMenu::getMenuId, rm -> rm, (a, b) -> a));

        // 构建菜单树
        List<MenuTreeNodeVO> treeNodes = allMenus.stream().map(menu -> {
            MenuTreeNodeVO node = new MenuTreeNodeVO();
            node.setId(menu.getId());
            node.setParentId(menu.getParentId());
            node.setMenuName(menu.getMenuName());
            node.setMenuType(menu.getMenuType());
            node.setIcon(menu.getIcon());
            node.setPosition(menu.getPosition());
            node.setOwner(menu.getOwner());
            node.setChildren(new ArrayList<>());

            SecurityRoleMenu bound = boundMenuMap.get(menu.getId());
            node.setDisabled(bound != null);
            node.setBoundRoleMenuId(bound != null ? bound.getId() : null);
            return node;
        }).toList();

        // 构建树形结构
        return buildTree(treeNodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveOrUpdateValidGroup(RoleValidGroupDTO dto) {
        SecurityRole role = super.getById(dto.getRoleId());
        if (role == null) {
            throw new BusinessException(SecurityErrorCode.E02001);
        }

        // 验证时效配置
        validateValidConfig(dto);

        if (dto.getRoleMenuId() != null && !dto.getRoleMenuId().isEmpty()) {
            // 更新现有时效组
            return updateValidGroup(dto, role);
        } else {
            // 新增时效组
            return createValidGroup(dto, role);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteValidGroup(String roleMenuId) {
        SecurityRoleMenu rm = roleMenuMapper.selectById(roleMenuId);
        if (rm == null) {
            return true;
        }

        // 查找相同时效组的所有记录
        List<SecurityRoleMenu> sameGroupRms = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SecurityRoleMenu>()
                                .eq(SecurityRoleMenu::getRoleId, rm.getRoleId()))
                .stream()
                .filter(r -> buildValidGroupKey(r).equals(buildValidGroupKey(rm)))
                .toList();

        List<String> rmIds = sameGroupRms.stream().map(SecurityRoleMenu::getId).toList();

        // 删除权限关联
        roleMenuPermissionMapper.delete(new LambdaQueryWrapper<SecurityRoleMenuPermission>()
                .in(SecurityRoleMenuPermission::getRoleMenuId, rmIds));

        // 删除角色菜单关联
        sameGroupRms.forEach(r -> roleMenuMapper.deleteById(r.getId()));

        return true;
    }

    // ==================== 私有方法 ====================

    private String buildValidGroupKey(SecurityRoleMenu rm) {
        return "%s_%s_%s_%s_%s_%s_%s".formatted(
                rm.getValidType() != null ? rm.getValidType().getCode() : "",
                rm.getValidStart() != null ? rm.getValidStart().toString() : "",
                rm.getValidEnd() != null ? rm.getValidEnd().toString() : "",
                rm.getCycleType() != null ? rm.getCycleType().getCode() : "",
                rm.getCycleValue() != null ? rm.getCycleValue() : "",
                rm.getCycleStartTime() != null ? rm.getCycleStartTime().toString() : "",
                rm.getCycleEndTime() != null ? rm.getCycleEndTime().toString() : ""
        );
    }

    private List<MenuTreeNodeVO> buildTree(List<MenuTreeNodeVO> nodes) {
        java.util.Map<String, MenuTreeNodeVO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(MenuTreeNodeVO::getId, n -> n));

        List<MenuTreeNodeVO> roots = new ArrayList<>();
        for (MenuTreeNodeVO node : nodes) {
            String parentId = node.getParentId();
            if (parentId == null || parentId.isEmpty() || SecurityMenu.ROOT_MENU_PARENT_ID.equals(parentId)) {
                roots.add(node);
            } else {
                MenuTreeNodeVO parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    private void validateValidConfig(RoleValidGroupDTO dto) {
        if (dto.getValidType() == ValidType.ABSOLUTE) {
            if (dto.getValidStart() == null || dto.getValidEnd() == null) {
                throw new BusinessException(SecurityErrorCode.E02005);
            }
            if (dto.getValidStart().isAfter(dto.getValidEnd())) {
                throw new BusinessException(SecurityErrorCode.E02005);
            }
        }
        if (dto.getValidType() == ValidType.CYCLE) {
            if (dto.getCycleType() == null || dto.getCycleValue() == null || dto.getCycleValue().isEmpty()) {
                throw new BusinessException(SecurityErrorCode.E02005);
            }
        }
    }

    private Boolean createValidGroup(RoleValidGroupDTO dto, SecurityRole role) {
        List<String> menuIds = dto.getMenuIds();
        if (CollectionUtils.isEmpty(menuIds)) {
            return true;
        }

        List<SecurityMenu> menus = menuMapper.selectByIds(menuIds);

        // 构建ExpressionContext
        ExpressionContext context = new ExpressionContext();
        context.setValue(role.getRoleCode());
        context.putExtraParam(RoleBindingMenuAbacLoading.ROLE_INFO_KEY, role);
        context.putExtraParam(RoleBindingMenuAbacLoading.MENUS_INFO_KEY, menus);
        context.putExtraParam("validType", dto.getValidType().getCode());

        if (dto.getValidType() == ValidType.ABSOLUTE) {
            context.putExtraParam("validStart", dto.getValidStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            context.putExtraParam("validEnd", dto.getValidEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        if (dto.getValidType() == ValidType.CYCLE) {
            context.putExtraParam("cycleType", dto.getCycleType().getCode());
            context.putExtraParam("cycleValue", dto.getCycleValue());
            context.putExtraParam("cycleStartTime", dto.getCycleStartTime() != null ? dto.getCycleStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
            context.putExtraParam("cycleEndTime", dto.getCycleEndTime() != null ? dto.getCycleEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        }

        // 处理权限
        permissionAlterationManager.alterationUrlPermission(AbacPerType.ROLE_BINDING_MENU, context);

        // 更新security_role_menu的时效字段
        List<SecurityRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getRoleId, role.getId())
                        .in(SecurityRoleMenu::getMenuId, menuIds));

        for (SecurityRoleMenu rm : roleMenus) {
            rm.setValidType(dto.getValidType());
            rm.setValidStart(dto.getValidStart());
            rm.setValidEnd(dto.getValidEnd());
            rm.setCycleType(dto.getCycleType());
            rm.setCycleValue(dto.getCycleValue());
            rm.setCycleStartTime(dto.getCycleStartTime());
            rm.setCycleEndTime(dto.getCycleEndTime());
            roleMenuMapper.updateById(rm);
        }

        return true;
    }

    private Boolean updateValidGroup(RoleValidGroupDTO dto, SecurityRole role) {
        // 获取此时效组关联的所有roleMenu记录（同一validGroupKey的记录）
        SecurityRoleMenu existingRm = roleMenuMapper.selectById(dto.getRoleMenuId());
        if (existingRm == null) {
            throw new BusinessException(SecurityErrorCode.E02001);
        }

        // 查找相同时效组的所有记录
        List<SecurityRoleMenu> sameGroupRms = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SecurityRoleMenu>()
                                .eq(SecurityRoleMenu::getRoleId, dto.getRoleId()))
                .stream()
                .filter(rm -> buildValidGroupKey(rm).equals(buildValidGroupKey(existingRm)))
                .toList();

        // 更新时效配置
        for (SecurityRoleMenu rm : sameGroupRms) {
            rm.setValidType(dto.getValidType());
            rm.setValidStart(dto.getValidStart());
            rm.setValidEnd(dto.getValidEnd());
            rm.setCycleType(dto.getCycleType());
            rm.setCycleValue(dto.getCycleValue());
            rm.setCycleStartTime(dto.getCycleStartTime());
            rm.setCycleEndTime(dto.getCycleEndTime());
            roleMenuMapper.updateById(rm);
        }

        // 处理菜单关联变更（如果menuIds有变化）
        List<String> newMenuIds = dto.getMenuIds() != null ? dto.getMenuIds() : Collections.emptyList();
        List<String> existingMenuIds = sameGroupRms.stream().map(SecurityRoleMenu::getMenuId).toList();

        // 需要新增的菜单
        List<String> toAdd = newMenuIds.stream()
                .filter(id -> !existingMenuIds.contains(id))
                .toList();
        // 需要移除的菜单
        List<String> toRemove = existingMenuIds.stream()
                .filter(id -> !newMenuIds.contains(id))
                .toList();

        // 移除菜单关联
        if (!toRemove.isEmpty()) {
            List<SecurityRoleMenu> removeRms = sameGroupRms.stream()
                    .filter(rm -> toRemove.contains(rm.getMenuId()))
                    .toList();
            List<String> removeRmIds = removeRms.stream().map(SecurityRoleMenu::getId).toList();

            // 删除权限关联
            roleMenuPermissionMapper.delete(new LambdaQueryWrapper<SecurityRoleMenuPermission>()
                    .in(SecurityRoleMenuPermission::getRoleMenuId, removeRmIds));
            // 删除角色菜单关联
            removeRms.forEach(rm -> roleMenuMapper.deleteById(rm.getId()));
        }

        // 新增菜单关联
        if (!toAdd.isEmpty()) {
            List<SecurityMenu> addMenus = menuMapper.selectByIds(toAdd);
            for (SecurityMenu menu : addMenus) {
                SecurityRoleMenu newRm = new SecurityRoleMenu()
                        .setRoleId(dto.getRoleId())
                        .setMenuId(menu.getId())
                        .setValidType(dto.getValidType())
                        .setValidStart(dto.getValidStart())
                        .setValidEnd(dto.getValidEnd())
                        .setCycleType(dto.getCycleType())
                        .setCycleValue(dto.getCycleValue())
                        .setCycleStartTime(dto.getCycleStartTime())
                        .setCycleEndTime(dto.getCycleEndTime());
                roleMenuMapper.insert(newRm);

                // 创建权限关联
                String permission = menu.getPermission();
                if (StringUtils.hasText(permission)) {
                    String[] urlPermissions = permission.split(";");
                    for (String urlPerm : urlPermissions) {
                        String p = urlPerm.trim().replace("(main)", "");
                        String[] tmp = p.split(":");
                        SecurityAbacPermission abacP = new SecurityAbacPermission()
                                .setId(IdUtil.getSnowflakeNextIdStr())
                                .setEffect(AbacEffect.PERMIT)
                                .setAction(tmp[0])
                                .setResourceType(tmp[1])
                                .setUrlPattern(tmp[2])
                                .setStatus(true);

                        SecurityRoleMenuPermission rmp = new SecurityRoleMenuPermission()
                                .setRoleMenuId(newRm.getId())
                                .setAbacPermissionId(abacP.getId());
                        roleMenuPermissionMapper.insert(rmp);
                    }
                }
            }
        }

        return true;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SecurityRole> buildQueryWrapper(RoleQueryDTO query) {
        LambdaQueryWrapper<SecurityRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityRole::getDeleted, false);

        if (query != null) {
            if (query.getRoleName() != null && !query.getRoleName().isEmpty()) {
                wrapper.like(SecurityRole::getRoleName, query.getRoleName());
            }
            if (query.getRoleCode() != null && !query.getRoleCode().isEmpty()) {
                wrapper.like(SecurityRole::getRoleCode, query.getRoleCode());
            }
            if (query.getStatus() != null) {
                wrapper.eq(SecurityRole::getStatus, query.getStatus());
            }
            if (query.getRoleType() != null) {
                wrapper.eq(SecurityRole::getRoleType, query.getRoleType());
            }
            if (query.getDataScope() != null) {
                wrapper.eq(SecurityRole::getDataScope, query.getDataScope());
            }
        }
        return wrapper;
    }
}
