package org.quyq.gwsu.system.config;

import org.quyq.gwsu.system.config.properties.ApiKeyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 系统模块配置
 *
 * @author Quyq
 */
@Configuration
@EnableConfigurationProperties(ApiKeyProperties.class)
public class SystemConfiguration {
}
