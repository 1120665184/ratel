package org.quyq.gwsu.common.security.config;

import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.security.collector.ApiEndpointCollector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * API 接口收集器配置 , 采集该服务的接口资源
 *
 * @author Quyq
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({RequestMappingHandlerMapping.class})
@ConditionalOnBean({RequestMappingHandlerMapping.class})
public class ApiEndpointCollectorConfiguration {

    @Bean
    public ApiEndpointCollector apiEndpointCollector(RequestMappingHandlerMapping handlerMapping,
                                                     CacheUtils cacheUtils , ProjectUtils projectUtils) {
        return new ApiEndpointCollector(handlerMapping, cacheUtils , projectUtils);
    }
}
