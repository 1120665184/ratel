package org.quyq.gwsu.security;


import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/3/10
 * @description
 */
@Component
public class SecurityModuleInfoProvider implements BusinessModuleInfoProvider {

    @Override
    public BusinessModuleInfo module() {
        return new BusinessModuleInfo("security" , "安全模块，包含权限、角色等内容");
    }
}
