package org.quyq.gwsu.headless;


import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.springframework.stereotype.Component;


/**
 * @author Quyq
 * @date 2026/6/25
 * @description
 */
@Component
public class HeadlessModuleInfoProvider implements BusinessModuleInfoProvider {
    @Override
    public BusinessModuleInfo module() {
        return new BusinessModuleInfo("headless" , "无头智能体服务");
    }
}
