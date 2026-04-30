package org.quyq.gwsu.common.authentication.login;


import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;
import org.quyq.gwsu.common.authentication.domain.LoginVO;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.login.domain.WebCallInfo;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.common.security.enums.VisitorType;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
public interface LoginHandler<T extends AbstractLoginDTO> {

    /**
     * 支持的登录类型
     *
     * @return
     */
    String loginType();

    default LoginVO authenticate(T loginDTO) {
        return authenticate(loginDTO, VisitorType.USER);
    }

    /**
     * 认证逻辑
     *
     * @param loginDTO    登录参数
     * @param visitorType 访问者类型，
     * @return
     */
    LoginVO authenticate(T loginDTO, @NonNull VisitorType visitorType);

    /**
     * 对于需要前端跳转的登录方式（如 OAuth2），生成对应的授权信息（url等）
     *
     * @param state
     * @return
     */
    default WebCallInfo generateWebCallInfo(String state) {
        throw new AuthException(CommonErrorCode.E04002);
    }

    /**
     * 登录的账号类型 , 用于系统中有多账号认证系统时使用
     *
     * @return
     */
    default AccountType accountType() {
        return AccountType.MANAGER;
    }


}
