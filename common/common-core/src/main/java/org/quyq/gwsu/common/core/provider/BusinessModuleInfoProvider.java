package org.quyq.gwsu.common.core.provider;


import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.springframework.core.env.Environment;

/**
 * @author Quyq
 * @date 2026/3/10
 * @description 业务模块基础信息配置
 */
public interface BusinessModuleInfoProvider {

    /**
     * 模块前缀名称
     *
     * @return
     */
    BusinessModuleInfo module();


    /**
     * 获取服务名
     *
     * @return
     */
    default String applicationName() {
        Environment environment = SpringUtils.getBean(Environment.class);

        return environment.getProperty(CoreConstants.Yaml.APPLICATION_NAME, "");
    }

}
