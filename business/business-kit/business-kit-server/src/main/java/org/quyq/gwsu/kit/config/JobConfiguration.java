package org.quyq.gwsu.kit.config;


import org.quyq.gwsu.kit.config.properties.JobAdminProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Quyq
 * @date 2026/7/7
 * @description
 */
@Configuration
@EnableConfigurationProperties(JobAdminProperties.class)
public class JobConfiguration {
}
