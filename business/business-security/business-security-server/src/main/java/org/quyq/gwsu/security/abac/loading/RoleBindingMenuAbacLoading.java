package org.quyq.gwsu.security.abac.loading;


import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.service.IAbacAlterationProvider;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionUrlWrapper;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;
import org.quyq.gwsu.security.api.role.enums.ValidType;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;
import org.quyq.gwsu.security.role.domain.SecurityRole;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenuPermission;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuPermissionMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/30
 * @description 角色绑定菜单时，从菜单中加载接口权限
 */
@Component
@RequiredArgsConstructor
public class RoleBindingMenuAbacLoading implements IAbacAlterationProvider {

    public static final String ROLE_INFO_KEY = "roleInfo";
    public static final String MENUS_INFO_KEY = "menusInfo";

    private final SecurityRoleMenuMapper roleMenuMapper;
    private final SecurityRoleMenuPermissionMapper roleMenuPermissionMapper;

    @Override
    public AbacPerType abacType() {
        return AbacPerType.ROLE_BINDING_MENU;
    }

    @Override
    public String buildExpression(ExpressionContext context) {
        String baseExpression = "contains(r.sub.roles , '%s')".formatted(context.getValue());

        // 获取时效配置
        Integer validType = context.getParam("validType");
        if (validType == null || validType == 1) {
            return baseExpression; // 永久，不追加
        }

        if (validType == 2) {
            // 绝对时间范围
            String validStart = context.getParam("validStart");
            String validEnd = context.getParam("validEnd");
            return baseExpression + " && timeInRange(\"%s\", \"%s\")".formatted(validStart, validEnd);
        }

        if (validType == 3) {
            // 周期性
            Integer cycleType = context.getParam("cycleType");
            String cycleValue = context.getParam("cycleValue");
            String cycleStartTime = context.getParam("cycleStartTime");
            String cycleEndTime = context.getParam("cycleEndTime");
            String startTime = cycleStartTime != null ? cycleStartTime : "";
            String endTime = cycleEndTime != null ? cycleEndTime : "";

            if (cycleType != null && cycleType == 1) {
                return baseExpression + " && cycleWeekly(\"%s\", \"%s\", \"%s\")".formatted(cycleValue, startTime, endTime);
            } else if (cycleType != null && cycleType == 2) {
                return baseExpression + " && cycleMonthly(\"%s\", \"%s\", \"%s\")".formatted(cycleValue, startTime, endTime);
            }
        }

        return baseExpression;
    }

    @Override
    public void alterationUrlPermission(ExpressionContext context, AbacPermissionUrlWrapper wrapper) {
        SecurityRole roleInfo = getRoleInfo(context);

        // 删除原有菜单关联和权限关联
        List<SecurityRoleMenu> existingRoleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getRoleId, roleInfo.getId()));

        List<String> existingRoleMenuIds = existingRoleMenus.stream()
                .map(SecurityRoleMenu::getId).toList();

        if (!existingRoleMenuIds.isEmpty()) {
            roleMenuPermissionMapper.delete(new LambdaQueryWrapper<SecurityRoleMenuPermission>()
                    .in(SecurityRoleMenuPermission::getRoleMenuId, existingRoleMenuIds));
        }

        roleMenuMapper.delete(new LambdaQueryWrapper<SecurityRoleMenu>()
                .eq(SecurityRoleMenu::getRoleId, roleInfo.getId()));

        // 删除绑定的abac权限
        wrapper.removeAll();

        List<SecurityMenu> menuInfos = getMenuInfos(context);
        if (CollectionUtils.isEmpty(menuInfos)) {
            return;
        }

        // 获取时效配置
        Integer validType = context.getParam("validType");

        List<SecurityAbacPermission> allPermissions = new ArrayList<>();
        List<SecurityRoleMenuPermission> allRoleMenuPermissions = new ArrayList<>();

        for (SecurityMenu menu : menuInfos) {
            // 构建带时效的角色-菜单关联
            AbacAndMenuRole result = buildAbac(roleInfo.getId(), menu, validType);

            // 保存角色菜单关联
            roleMenuMapper.insert(result.roleMenu());

            // 收集abac权限
            if (!CollectionUtils.isEmpty(result.permissions())) {
                allPermissions.addAll(result.permissions());
            }

            // 收集角色菜单权限关联
            if (!CollectionUtils.isEmpty(result.roleMenuPermissions())) {
                allRoleMenuPermissions.addAll(result.roleMenuPermissions());
            }
        }

        // 保存abac权限
        wrapper.addPermissions(allPermissions);

        // 保存角色菜单权限关联
        allRoleMenuPermissions.forEach(rmp -> roleMenuPermissionMapper.insert(rmp));
    }


    private AbacAndMenuRole buildAbac(String roleId, SecurityMenu menu, Integer validType) {
        // 创建 SecurityRoleMenu（一个菜单一条记录，含时效）
        SecurityRoleMenu roleMenu = new SecurityRoleMenu()
                .setRoleId(roleId)
                .setMenuId(menu.getId())
                .setValidType(validType != null ? ValidType.of(validType) : ValidType.PERMANENT);

        // TODO: 时效字段从context获取后设置（当前assignMenus场景validType=1，详细时效由saveOrUpdateValidGroup处理）

        String permission = menu.getPermission();
        if (!StringUtils.hasText(permission)) {
            return new AbacAndMenuRole(roleMenu, Collections.emptyList(), Collections.emptyList());
        }

        String[] urlPermissions = permission.split(";");

        List<SecurityAbacPermission> permissions = new ArrayList<>();
        List<SecurityRoleMenuPermission> roleMenuPermissions = new ArrayList<>();

        for (String urlPermission : urlPermissions) {
            String p = urlPermission.trim().replace("(main)", "");
            String[] tmp = p.split(":");

            SecurityAbacPermission abacP = new SecurityAbacPermission()
                    .setId(IdUtil.getSnowflakeNextIdStr())
                    .setEffect(AbacEffect.PERMIT)
                    .setAction(tmp[0])
                    .setResourceType(tmp[1])
                    .setUrlPattern(tmp[2])
                    .setStatus(true);

            permissions.add(abacP);

            SecurityRoleMenuPermission rmp = new SecurityRoleMenuPermission()
                    .setRoleMenuId(roleMenu.getId())
                    .setAbacPermissionId(abacP.getId());
            roleMenuPermissions.add(rmp);
        }

        return new AbacAndMenuRole(roleMenu, permissions, roleMenuPermissions);
    }


    /**
     * 获取菜单信息列表
     *
     * @param context
     * @return
     */
    public List<SecurityMenu> getMenuInfos(ExpressionContext context) {
        return context.getParam(MENUS_INFO_KEY);
    }

    /**
     * 获取角色信息
     *
     * @param context
     * @return
     */
    public SecurityRole getRoleInfo(ExpressionContext context) {
        return context.getParam(ROLE_INFO_KEY);
    }


    protected record AbacAndMenuRole(SecurityRoleMenu roleMenu,
                                     List<SecurityAbacPermission> permissions,
                                     List<SecurityRoleMenuPermission> roleMenuPermissions) {
    }


}
