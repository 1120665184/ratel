package org.quyq.gwsu.security.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.api.role.dto.RoleQueryDTO;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.quyq.gwsu.security.role.domain.SecurityRole;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuMapper;
import org.quyq.gwsu.security.role.service.ISecurityRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        return saveOrUpdate(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeByIds(List<String> ids) {
        // 删除角色菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SecurityRoleMenu>()
                .in(SecurityRoleMenu::getRoleId, ids));
        // 删除角色
        return removeBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignMenus(String roleId, List<String> menuIds) {
        // 删除原有菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SecurityRoleMenu>()
                .eq(SecurityRoleMenu::getRoleId, roleId));

        // 新增菜单关联
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SecurityRoleMenu> roleMenus = menuIds.stream()
                    .map(menuId -> {
                        SecurityRoleMenu roleMenu = new SecurityRoleMenu();
                        roleMenu.setRoleId(roleId);
                        roleMenu.setMenuId(menuId);
                        return roleMenu;
                    })
                    .toList();

            for (SecurityRoleMenu roleMenu : roleMenus) {
                roleMenuMapper.insert(roleMenu);
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
        }
        return wrapper;
    }
}
