package org.quyq.gwsu.system.manager.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.system.api.manager.dto.SysAccountBindDTO;
import org.quyq.gwsu.system.api.manager.dto.SysUserQueryDTO;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.api.manager.vo.UserVO;
import org.quyq.gwsu.system.manager.domain.SysUser;

import java.util.List;

public interface ISysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    IPage<UserVO> pageByCondition(SysUserQueryDTO query);

    SysUserDetailVO getDetailById(String id);

    String saveOrUpdateUser(UserVO vo);

    void updateStatus(String id, Integer status);

    void bindAccount(String userId, SysAccountBindDTO dto);

    void unbindAccount(String userId, String accountId);

    void removeUsers(List<String> ids);

    void resetPassword(String userId, String newPassword);
}
