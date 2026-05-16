package org.quyq.gwsu.security.role.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.role.dto.RoleTableModelSaveDTO;
import org.quyq.gwsu.security.api.apiresource.dto.TableModelQueryDTO;
import org.quyq.gwsu.security.api.role.vo.RoleTableModelVO;
import org.quyq.gwsu.security.role.domain.SecurityRoleTableModel;

import java.util.List;
import java.util.Map;

/**
 * 角色表模型权限服务接口
 */
public interface ISecurityRoleTableModelService extends IService<SecurityRoleTableModel> {

    /**
     * 分页查询
     */
    IPage<RoleTableModelVO> pageByCondition(TableModelQueryDTO query);

    /**
     * 根据角色ID查询表模型权限列表
     */
    List<RoleTableModelVO> listByRoleId(String roleId);

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
     * @param roleCodes 角色code列表
     */
    Map<String, Map<String, FieldPermission>> getMergedRoleTableModelPermission(List<String> roleCodes);
}
