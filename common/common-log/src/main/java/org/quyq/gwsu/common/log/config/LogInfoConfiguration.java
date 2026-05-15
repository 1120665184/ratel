package org.quyq.gwsu.common.log.config;


import org.quyq.gwsu.common.log.config.properties.LogInfoConfigProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Quyq
 * @date 2026/5/14
 * @description
 */
@AutoConfiguration
@EnableConfigurationProperties(LogInfoConfigProperties.class)
public class LogInfoConfiguration {
}
