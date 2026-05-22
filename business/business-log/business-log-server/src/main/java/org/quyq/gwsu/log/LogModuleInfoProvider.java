package org.quyq.gwsu.log;

import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.springframework.stereotype.Component;

/**
 * 日志模块信息提供者
 *
 * @author Quyq
 */
@Component
public class LogModuleInfoProvider implements BusinessModuleInfoProvider {

    @Override
    public BusinessModuleInfo module() {
        return new BusinessModuleInfo("log", "日志模块");
    }
}
