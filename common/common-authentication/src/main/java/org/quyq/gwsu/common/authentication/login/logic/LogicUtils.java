package org.quyq.gwsu.common.authentication.login.logic;


import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.security.enums.AccountType;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
class LogicUtils {

    private LogicUtils() {
    }

    /**
     * 获取当前认证的Logic
     *
     * @return
     */
    protected static Optional<StpLogic> getLogic() {
        AccountType accountType = AuthenticationConstants.ACCOUNT_TYPE.get();
        if (Objects.isNull(accountType)) {
            return Optional.empty();
        }
        return Optional.of(SaManager.getStpLogic(accountType.name(), true));
    }
}