package org.quyq.gwsu.security.role.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.role.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.role.vo.RolePermissionTableModelVO;
import org.quyq.gwsu.security.role.domain.SecurityRoleTableModel;

import java.util.List;
import java.util.Map;

/**
 * 角色表模型权限服务接口
 */
public interface ISecurityRoleTableModelService extends IService<SecurityRoleTableModel> {


    /**
     * 保存或更新角色表模型权限
     */
    Boolean saveOrUpdateRoleTableModel(RoleTableModelSaveDTO dto);

    /**
     * 批量删除
     */
    Boolean removeByIds(List<String> ids);

    /**
     * 获取指定角色的合并后表模型权限
     * key = "module_prefix:datasource:table_name"
     *
     * @param roleCodes 角色code列表
     */
    Map<String, Map<String, FieldPermission>> getMergedRoleTableModelPermission(List<String> roleCodes);


    /**
     * 通过角色ID查询出该角色拥有的表模型权限
     * @return
     */
    List<RolePermissionTableModelVO> getTableModelPermission(String roleId);

}
