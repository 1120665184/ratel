package org.quyq.gwsu.security.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.loading.RoleBindingMenuAbacLoading;
import org.quyq.gwsu.security.abac.service.PermissionAlterationManager;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.enums.MenuPosition;
import org.quyq.gwsu.security.api.role.dto.RoleQueryDTO;
import org.quyq.gwsu.security.api.role.dto.RoleValidGroupDTO;
import org.quyq.gwsu.security.api.role.enums.RoleType;
import org.quyq.gwsu.security.api.role.enums.ValidType;
import org.quyq.gwsu.security.api.role.vo.MenuTreeNodeVO;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.quyq.gwsu.security.api.role.vo.RoleValidGroupVO;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;
import org.quyq.gwsu.security.menu.mapper.SecurityMenuMapper;
import org.quyq.gwsu.security.role.domain.SecurityRole;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.domain.SecurityRoleSubject;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleSubjectMapper;
import org.quyq.gwsu.security.role.service.ISecurityRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityRoleServiceImpl extends ServiceImpl<SecurityRoleMapper, SecurityRole> implements ISecurityRoleService {

    private static final Comparator<SecurityMenu> MENU_TREE_COMPARATOR = Comparator
            .comparingInt(SecurityRoleServiceImpl::menuPositionOrder)
            .thenComparing(SecurityMenu::getSort, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(SecurityMenu::getId, Comparator.nullsLast(String::compareTo));

    private final SecurityRoleMenuMapper roleMenuMapper;

    private final SecurityRoleSubjectMapper roleSubjectMapper;

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
        wrapper.orderByAsc( SecurityRole::getRoleType,SecurityRole::getSort);

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
    public List<RoleVO> roleByIdents(List<String> idents) {
        return baseMapper.selectList(new LambdaQueryWrapper<SecurityRole>()
                .in(SecurityRole::getRoleCode , idents)
        ).stream().map(SecurityRole::toVo).toList();
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

        // 先删除每个角色关联的ABAC表达式权限（需要先查后删，因为wrapper按表达式隔离）
        for (String roleId : ids) {
            List<SecurityRoleMenu> roleMenus = roleMenuMapper.selectList(
                    new LambdaQueryWrapper<SecurityRoleMenu>()
                            .eq(SecurityRoleMenu::getRoleId, roleId));

            if (!CollectionUtils.isEmpty(roleMenus)) {
                SecurityRole role = baseMapper.selectById(roleId);
                if (role != null) {
                    // 按 validGroupKey 分组，每组构建表达式并删除
                    Map<String, List<SecurityRoleMenu>> grouped = roleMenus.stream()
                            .collect(Collectors.groupingBy(this::buildValidGroupKey));
                    for (List<SecurityRoleMenu> group : grouped.values()) {
                        SecurityRoleMenu first = group.getFirst();
                        ExpressionContext ctx = buildExpressionContext(role, first);
                        // 传入空菜单列表，alterationUrlPermission 内部会先删旧数据（role_menu + role_menu_permission + abac_permission），不插入新数据
                        ctx.putExtraParam(RoleBindingMenuAbacLoading.MENUS_INFO_KEY, Collections.emptyList());
                        permissionAlterationManager.alterationUrlPermission(AbacPerType.ROLE_BINDING_MENU, ctx);
                    }
                }
            }
        }

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

        // 补充父节点菜单ID
        List<String> allMenuIds = collectWithParentIds(menuIds);

        ExpressionContext context = new ExpressionContext();
        context.setValue(role.getRoleCode());
        context.putExtraParam(RoleBindingMenuAbacLoading.ROLE_INFO_KEY, role);
        context.putExtraParam(RoleBindingMenuAbacLoading.VALID_TYPE_KEY, ValidType.PERMANENT);

        List<SecurityMenu> menus = Collections.emptyList();
        if (!CollectionUtils.isEmpty(allMenuIds)) {
            menus = menuMapper.selectByIds(allMenuIds);
        }
        context.putExtraParam(RoleBindingMenuAbacLoading.MENUS_INFO_KEY, menus);

        // 通过 PermissionAlterationManager 处理权限
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
                .eq(SecurityMenu::getStatus, true);
        if (owner != null) {
            wrapper.eq(SecurityMenu::getOwner, owner);
        }
        List<SecurityMenu> allMenus = menuMapper.selectList(wrapper).stream()
                .sorted(MENU_TREE_COMPARATOR)
                .toList();

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

        if (StringUtils.hasText(dto.getRoleMenuId())) {
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

        SecurityRole role = baseMapper.selectById(rm.getRoleId());
        if (role == null) {
            return true;
        }

        // 构建 context 传入时效信息，通过 PermissionAlterationManager 删除
        // 传入空的菜单列表，alterationUrlPermission 会先删旧数据，发现新数据为空就只删不增
        ExpressionContext context = buildExpressionContext(role, rm);
        context.putExtraParam(RoleBindingMenuAbacLoading.MENUS_INFO_KEY, Collections.emptyList());

        permissionAlterationManager.alterationUrlPermission(AbacPerType.ROLE_BINDING_MENU, context);

        return true;
    }

    @Override
    public void allocationRoleToSubject(String subjectId, List<String> roleIds) {
        roleSubjectMapper.delete(new LambdaQueryWrapper<SecurityRoleSubject>()
                .eq(SecurityRoleSubject::getSubjectId, subjectId));

        if(CollectionUtils.isEmpty(roleIds)) {
            return;
        }

        Set<SecurityRoleSubject> rs = roleIds.stream().map(roleId ->
                new SecurityRoleSubject()
                        .setRoleId(roleId)
                        .setSubjectId(subjectId)
        ).collect(Collectors.toSet());

        roleSubjectMapper.insert(rs);
    }

    @Override
    public void allocationSubjectToRole(String roleId, List<String> subjectIds) {
        roleSubjectMapper.delete(new LambdaQueryWrapper<SecurityRoleSubject>()
                .eq(SecurityRoleSubject::getRoleId, roleId));

        if(CollectionUtils.isEmpty(subjectIds)) {
            return;
        }

        Set<SecurityRoleSubject> rs = subjectIds.stream().map(subjectId ->
                new SecurityRoleSubject()
                        .setRoleId(roleId)
                        .setSubjectId(subjectId)
        ).collect(Collectors.toSet());

        roleSubjectMapper.insert(rs);

    }

    @Override
    public List<String> listSubjectIdsByRoleId(String roleId) {
        List<SecurityRoleSubject> list = roleSubjectMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleSubject>()
                        .eq(SecurityRoleSubject::getRoleId, roleId));
        return list.stream()
                .map(SecurityRoleSubject::getSubjectId)
                .toList();
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

    /**
     * 根据 SecurityRoleMenu 的时效信息构建 ExpressionContext
     */
    private ExpressionContext buildExpressionContext(SecurityRole role, SecurityRoleMenu rm) {
        ExpressionContext context = new ExpressionContext();
        context.setValue(role.getRoleCode());
        context.putExtraParam(RoleBindingMenuAbacLoading.ROLE_INFO_KEY, role);
        context.putExtraParam(RoleBindingMenuAbacLoading.VALID_TYPE_KEY,
                rm.getValidType() != null ? rm.getValidType() : ValidType.PERMANENT);

        if (rm.getValidType() == ValidType.ABSOLUTE) {
            context.putExtraParam(RoleBindingMenuAbacLoading.VALID_START_KEY,
                    rm.getValidStart() != null ? rm.getValidStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
            context.putExtraParam(RoleBindingMenuAbacLoading.VALID_END_KEY,
                    rm.getValidEnd() != null ? rm.getValidEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
        } else if (rm.getValidType() == ValidType.CYCLE) {
            context.putExtraParam(RoleBindingMenuAbacLoading.CYCLE_TYPE_KEY, rm.getCycleType());
            context.putExtraParam(RoleBindingMenuAbacLoading.CYCLE_VALUE_KEY, rm.getCycleValue());
            context.putExtraParam(RoleBindingMenuAbacLoading.CYCLE_START_TIME_KEY, rm.getCycleStartTime());
            context.putExtraParam(RoleBindingMenuAbacLoading.CYCLE_END_TIME_KEY, rm.getCycleEndTime());
        }
        return context;
    }

    /**
     * 根据 RoleValidGroupDTO 的时效信息构建 ExpressionContext
     */
    private ExpressionContext buildExpressionContext(SecurityRole role, RoleValidGroupDTO dto, List<SecurityMenu> menus) {
        ExpressionContext context = new ExpressionContext();
        context.setValue(role.getRoleCode());
        context.putExtraParam(RoleBindingMenuAbacLoading.ROLE_INFO_KEY, role);
        context.putExtraParam(RoleBindingMenuAbacLoading.MENUS_INFO_KEY, menus);
        context.putExtraParam(RoleBindingMenuAbacLoading.VALID_TYPE_KEY,
                dto.getValidType() != null ? dto.getValidType() : ValidType.PERMANENT);

        if (dto.getValidType() == ValidType.ABSOLUTE) {
            context.putExtraParam(RoleBindingMenuAbacLoading.VALID_START_KEY,
                    dto.getValidStart() != null ? dto.getValidStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
            context.putExtraParam(RoleBindingMenuAbacLoading.VALID_END_KEY,
                    dto.getValidEnd() != null ? dto.getValidEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
        } else if (dto.getValidType() == ValidType.CYCLE) {
            context.putExtraParam(RoleBindingMenuAbacLoading.CYCLE_TYPE_KEY, dto.getCycleType());
            context.putExtraParam(RoleBindingMenuAbacLoading.CYCLE_VALUE_KEY, dto.getCycleValue());
            context.putExtraParam(RoleBindingMenuAbacLoading.CYCLE_START_TIME_KEY, dto.getCycleStartTime());
            context.putExtraParam(RoleBindingMenuAbacLoading.CYCLE_END_TIME_KEY, dto.getCycleEndTime());
        }
        return context;
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

    private static int menuPositionOrder(SecurityMenu menu) {
        MenuPosition position = menu.getPosition();
        if (position == MenuPosition.HEADER) {
            return 0;
        }
        if (position == MenuPosition.SIDEBAR) {
            return 1;
        }
        return Integer.MAX_VALUE;
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

    /**
     * 校验新增时效组是否与已有时效组重复
     */
    private void validateValidGroupDuplicate(String roleId, RoleValidGroupDTO dto) {
        List<SecurityRoleMenu> existingRoleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getRoleId, roleId));

        // 构建新时效组的 key
        String newGroupKey = buildValidGroupKeyFromDto(dto);

        for (SecurityRoleMenu rm : existingRoleMenus) {
            String existingKey = buildValidGroupKey(rm);
            if (newGroupKey.equals(existingKey)) {
                throw new BusinessException(SecurityErrorCode.E02006);
            }
        }
    }

    /**
     * 根据 DTO 构建时效组 key
     */
    private String buildValidGroupKeyFromDto(RoleValidGroupDTO dto) {
        return "%s_%s_%s_%s_%s_%s_%s".formatted(
                dto.getValidType() != null ? dto.getValidType().getCode() : "",
                dto.getValidStart() != null ? dto.getValidStart().toString() : "",
                dto.getValidEnd() != null ? dto.getValidEnd().toString() : "",
                dto.getCycleType() != null ? dto.getCycleType().getCode() : "",
                dto.getCycleValue() != null ? dto.getCycleValue() : "",
                dto.getCycleStartTime() != null ? dto.getCycleStartTime() : "",
                dto.getCycleEndTime() != null ? dto.getCycleEndTime() : ""
        );
    }

    private Boolean createValidGroup(RoleValidGroupDTO dto, SecurityRole role) {
        List<String> menuIds = dto.getMenuIds();
        if (CollectionUtils.isEmpty(menuIds)) {
            return true;
        }

        // 校验时效组是否重复
        validateValidGroupDuplicate(role.getId(), dto);

        // 补充父节点菜单ID
        List<String> allMenuIds = collectWithParentIds(menuIds);
        List<SecurityMenu> menus = menuMapper.selectByIds(allMenuIds);

        // 构建 ExpressionContext 并通过 PermissionAlterationManager 统一处理
        ExpressionContext context = buildExpressionContext(role, dto, menus);
        permissionAlterationManager.alterationUrlPermission(AbacPerType.ROLE_BINDING_MENU, context);

        return true;
    }

    private Boolean updateValidGroup(RoleValidGroupDTO dto, SecurityRole role) {
        // 更新时效组：先删旧数据，再插新数据，全部通过 PermissionAlterationManager 处理

        // 补充父节点菜单ID
        List<String> rawMenuIds = dto.getMenuIds() != null ? dto.getMenuIds() : Collections.emptyList();
        List<String> allMenuIds = collectWithParentIds(rawMenuIds);
        List<SecurityMenu> menus = menuMapper.selectByIds(allMenuIds);

        // 构建 ExpressionContext（新时效配置）
        ExpressionContext context = buildExpressionContext(role, dto, menus);

        // 如果时效配置发生了变化，需要先删除旧时效组的数据
        SecurityRoleMenu existingRm = roleMenuMapper.selectById(dto.getRoleMenuId());
        if (existingRm != null) {
            String oldGroupKey = buildValidGroupKey(existingRm);
            String newGroupKey = buildValidGroupKeyFromDto(dto);
            if (!oldGroupKey.equals(newGroupKey)) {
                // 时效配置变更，需要校验新时效组是否与已有其他组重复
                validateValidGroupDuplicate(role.getId(), dto);

                // 先删除旧时效组的ABAC表达式权限
                ExpressionContext oldContext = buildExpressionContext(role, existingRm);
                oldContext.putExtraParam(RoleBindingMenuAbacLoading.MENUS_INFO_KEY, Collections.emptyList());
                permissionAlterationManager.alterationUrlPermission(AbacPerType.ROLE_BINDING_MENU, oldContext);
            }
        }

        // 通过 PermissionAlterationManager 保存新数据（内部先删后插）
        permissionAlterationManager.alterationUrlPermission(AbacPerType.ROLE_BINDING_MENU, context);

        return true;
    }

    /**
     * 补充父节点菜单ID
     * 根据传入的菜单ID列表，自动查找所有父级菜单ID并合并返回
     *
     * @param menuIds 原始菜单ID列表
     * @return 包含所有父节点的完整菜单ID列表
     */
    private List<String> collectWithParentIds(List<String> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return menuIds;
        }
        Set<String> result = new HashSet<>(menuIds);
        // 查询传入的菜单，获取其 parentId
        List<SecurityMenu> menus = menuMapper.selectByIds(menuIds);
        Set<String> needLookup = new HashSet<>();
        for (SecurityMenu menu : menus) {
            String parentId = menu.getParentId();
            if (parentId != null && !parentId.isEmpty() && !SecurityMenu.ROOT_MENU_PARENT_ID.equals(parentId)) {
                needLookup.add(parentId);
            }
        }
        // 逐层向上查找父节点，直到到达根节点
        while (!needLookup.isEmpty()) {
            List<SecurityMenu> parentMenus = menuMapper.selectByIds(new ArrayList<>(needLookup));
            needLookup.clear();
            for (SecurityMenu parent : parentMenus) {
                if (result.add(parent.getId())) {
                    // 新加入的父节点，继续向上查找
                    String grandParentId = parent.getParentId();
                    if (grandParentId != null && !grandParentId.isEmpty()
                            && !SecurityMenu.ROOT_MENU_PARENT_ID.equals(grandParentId)) {
                        needLookup.add(grandParentId);
                    }
                }
            }
        }
        return new ArrayList<>(result);
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
