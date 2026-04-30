package org.quyq.gwsu.common.authentication.constants;


import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.security.enums.AccountType;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
public interface AuthenticationConstants {

    /**
     * 账号类型记录
     */
    ScopedValue<AccountType> ACCOUNT_TYPE = ScopedValue.newInstance();

    /**
     * 登录类型
     */
    interface LoginType {

        String PASSWORD = "password";

    }

    /**
     * 数据资源
     */
    interface DataResource {

        /**
         * 默认工作区
         */
        WorkspaceInfo DEFAULT_WORKSPACE = new WorkspaceInfo("Default" , "默认");
    }


}
