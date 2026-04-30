package org.quyq.gwsu.security.role.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.security.api.role.dto.RoleQueryDTO;
import org.quyq.gwsu.security.api.role.vo.RoleVO;
import org.quyq.gwsu.security.role.domain.SecurityRole;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author Quyq
 */
public interface ISecurityRoleService extends IService<SecurityRole> {

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 角色信息
     */
    RoleVO getById(String id);

    /**
     * 根据角色编码查询
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    RoleVO getByCode(String roleCode);

    /**
     * 分页查询角色
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<RoleVO> pageByCondition(RoleQueryDTO query);

    /**
     * 查询角色列表
     *
     * @param query 查询条件
     * @return 角色列表
     */
    List<RoleVO> listByCondition(RoleQueryDTO query);

    /**
     * 根据主体ID查询角色列表
     *
     * @param subjectId 主体ID
     * @return 角色列表
     */
    List<RoleVO> listBySubjectId(String subjectId);

    /**
     * 保存或更新角色
     *
     * @param role 角色信息
     * @return 是否成功
     */
    Boolean saveOrUpdateRole(SecurityRole role);

    /**
     * 批量删除角色
     *
     * @param ids 角色ID列表
     * @return 是否成功
     */
    Boolean removeByIds(List<String> ids);

    /**
     * 分配角色菜单
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     * @return 是否成功
     */
    Boolean assignMenus(String roleId, List<String> menuIds);
}
