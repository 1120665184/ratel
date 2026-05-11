package org.quyq.gwsu.security.menu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.menu.dto.MenuQueryDTO;
import org.quyq.gwsu.security.api.menu.dto.MenuSortDTO;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author Quyq
 */
public interface ISecurityMenuService extends IService<SecurityMenu> {

    /**
     * 根据ID查询菜单
     *
     * @param id 菜单ID
     * @return 菜单信息
     */
    MenuVO getById(String id);

    /**
     * 查询菜单树
     *
     * @param query 查询条件
     * @param owner 菜单所属类型
     * @param showButton 是否显示按钮
     * @return 菜单树
     */
    List<MenuVO> listTree(MenuQueryDTO query, MenuOwner owner , boolean showButton);

    /**
     * 根据用户ID查询菜单树
     *
     * @param subjectId 主体ID（用户ID）
     * @param owner 菜单所属类型
     * @return 菜单树
     */
    List<MenuVO> listTreeBySubjectId(String subjectId, MenuOwner owner);

    /**
     * 根据角色ID查询菜单ID列表
     *
     * @param roleId 角色ID
     * @param owner 菜单所属类型
     * @return 菜单ID列表
     */
    List<String> listMenuIdsByRoleId(String roleId, MenuOwner owner);

    /**
     * 保存或更新菜单
     *
     * @param menu 菜单信息
     * @return 是否成功
     */
    Boolean saveOrUpdateMenu(SecurityMenu menu);

    /**
     * 批量删除菜单
     *
     * @param ids 菜单ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);

    /**
     * 获取当前用户路由菜单
     * 超级管理员返回所有菜单，普通用户返回角色关联菜单
     *
     * @param owner 菜单所属类型
     */
    List<MenuVO> listUserRoutes(MenuOwner owner);

    /**
     * 根据角色编码列表查询菜单树
     *
     * @param roleCodes 角色编码列表
     * @param owner 菜单所属类型
     * @return 菜单树
     */
    List<MenuVO> listTreeByRoleCodes(List<String> roleCodes, MenuOwner owner);

    /**
     * 获取指定菜单下的按钮列表
     *
     * @param parentId 父菜单ID
     * @param owner    菜单所属类型
     * @return 按钮列表（平铺，无树结构）
     */
    List<MenuVO> listButtonsByParentId(String parentId, MenuOwner owner);

    /**
     * 批量更新菜单排序和父级
     *
     * @param sortItems 排序项列表
     * @return 是否成功
     */
    Boolean batchSort(List<MenuSortDTO> sortItems);
}
