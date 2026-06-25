package org.quyq.gwsu.headless;


import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;


/**
 * @author Quyq
 * @date 2026/6/25
 * @description
 */
public class HeadlessModuleInfoProvider implements BusinessModuleInfoProvider {
    @Override
    public BusinessModuleInfo module() {
        return new BusinessModuleInfo("connect" , "无头智能体服务");
    }
}
