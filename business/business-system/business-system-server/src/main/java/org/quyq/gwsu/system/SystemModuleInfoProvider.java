package org.quyq.gwsu.system;

import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @description 系统模块信息提供者
 */
@Component
public class SystemModuleInfoProvider implements BusinessModuleInfoProvider {

    @Override
    public BusinessModuleInfo module() {
        return new BusinessModuleInfo(SecurityConstants.Authentication.AUTH_SERVER_PREFIX, "系统模块");
    }
}
