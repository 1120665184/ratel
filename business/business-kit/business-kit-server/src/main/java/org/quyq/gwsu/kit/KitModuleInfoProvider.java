package org.quyq.gwsu.kit;


import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/6/5
 * @description
 */
@Component
public class KitModuleInfoProvider implements BusinessModuleInfoProvider {
    @Override
    public BusinessModuleInfo module() {
        return new BusinessModuleInfo("kit" , "工具套件服务");
    }
}
