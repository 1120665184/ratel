package org.quyq.gwsu.system.dept.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.system.api.dept.dto.RemoveUserDeptDTO;
import org.quyq.gwsu.system.api.dept.dto.SetPrimaryDeptDTO;
import org.quyq.gwsu.system.api.dept.dto.UserDeptSaveDTO;
import org.quyq.gwsu.system.api.dept.vo.UserDeptDetailVO;
import org.quyq.gwsu.system.dept.domain.SysUserDept;

import java.util.List;

/**
 * 用户部门关联服务接口
 *
 * @author Quyq
 */
public interface ISysUserDeptService extends IService<SysUserDept> {

    /**
     * 设置用户部门
     *
     * @param dto 设置请求
     */
    void saveUserDept(UserDeptSaveDTO dto);

    /**
     * 设置主部门
     *
     * @param dto 设置请求
     */
    void setPrimaryDept(SetPrimaryDeptDTO dto);

    /**
     * 移除用户部门
     *
     * @param dto 移除请求
     */
    void removeUserDept(RemoveUserDeptDTO dto);

    /**
     * 获取用户所属部门列表
     *
     * @param userId 用户ID
     * @return 用户部门详情列表
     */
    List<UserDeptDetailVO> listDeptsByUser(String userId);
}