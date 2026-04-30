package org.quyq.gwsu.system.login;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.login.impl.PasswordLoginDTO;
import org.quyq.gwsu.common.authentication.login.impl.PasswordLoginHandler;
import org.quyq.gwsu.system.api.manager.vo.SysUserDetailVO;
import org.quyq.gwsu.system.errcode.SystemErrorCode;
import org.quyq.gwsu.system.manager.domain.SysAccount;
import org.quyq.gwsu.system.manager.service.ISysAccountService;
import org.quyq.gwsu.system.manager.service.ISysUserService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * 系统用户密码登录处理器
 *
 * @author Quyq
 */
@Component
@RequiredArgsConstructor
public class SysUserPasswordLoginHandler extends PasswordLoginHandler<SysUserDetailVO> {

    private final ISysAccountService accountService;
    private final ISysUserService userService;

    @Override
    protected SysUserDetailVO toAuth(PasswordLoginDTO loginVO, CoreProperties properties) {
        // 1. 查询账号
        SysAccount account = accountService.getByIdentifier(
                PasswordLoginDTO.LOGIN_TYPE,
                loginVO.getUsername()
        );
        if (account == null) {
            throw new AuthException(loginVO.getUsername(), SystemErrorCode.E00001);
        }

        // 2. 验证密码
        if (account.getCredential() == null ||
                !BCrypt.checkpw(loginVO.getPassword(), account.getCredential())) {
            throw new AuthException(loginVO.getUsername(), SystemErrorCode.E00001);
        }

        // 3. 检查账号状态
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new AuthException(loginVO.getUsername(), SystemErrorCode.E00002);
        }

        // 4. 查询用户

        SysUserDetailVO user = userService.getDetailById(account.getUserId());
        if (user == null) {
            throw new AuthException(loginVO.getUsername(), SystemErrorCode.E00003);
        }

        // 5. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new AuthException(loginVO.getUsername(), SystemErrorCode.E00004);
        }

        return user;
    }

}
