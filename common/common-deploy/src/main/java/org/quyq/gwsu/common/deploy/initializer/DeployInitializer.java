package org.quyq.gwsu.common.deploy.initializer;


import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.utils.ProxyUtil;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/3/11
 * @description 部署模式属性配置
 */
public class DeployInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        Map<String, Object> deployConfig = new HashMap<>();

        deployConfig.put(CoreConstants.Yaml.DEPLOY_SINGLE , !ProxyUtil.hasClass("com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration"));

        environment.getPropertySources().addFirst(new MapPropertySource("deploy-config" , deployConfig));

    }
}
