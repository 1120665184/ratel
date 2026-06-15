package org.quyq.gwsu.common.authentication.login.impl.password;


import org.quyq.gwsu.common.authentication.login.AbstractLoginHandler;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
public abstract class PasswordLoginHandler<T extends UserInfo> extends AbstractLoginHandler<PasswordLoginDTO, T> {


    @Override
    public String loginType() {
        return PasswordLoginDTO.LOGIN_TYPE;
    }
}
