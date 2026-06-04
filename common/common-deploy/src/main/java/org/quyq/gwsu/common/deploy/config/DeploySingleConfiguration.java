package org.quyq.gwsu.common.deploy.config;


import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.core.utils.filter.ProcessorChain;
import org.quyq.gwsu.common.deploy.aop.ReactorContextCaptureAspect;
import org.quyq.gwsu.common.deploy.controller.SingleModuleController;
import org.quyq.gwsu.common.deploy.filter.SingleProcessorFilter;
import org.quyq.gwsu.common.deploy.mvc.SingleAppRouteHandlerConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/3/11
 * @description
 */
@AutoConfiguration
@ConditionalOnProperty(name = CoreConstants.Yaml.DEPLOY_SINGLE, havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DeploySingleConfiguration {

    @Bean
    public SingleAppRouteHandlerConfiguration singleAppRouteHandlerConfiguration(ObjectProvider<List<BusinessModuleInfoProvider>> providers) {
        return new SingleAppRouteHandlerConfiguration(providers.getIfAvailable());
    }

    @Bean
    public SingleModuleController singleModuleController(ObjectProvider<List<BusinessModuleInfoProvider>> providers) {
        return new SingleModuleController(providers.getIfAvailable());
    }

    @Bean
    public SingleProcessorFilter singleProcessorFilter(ProcessorChain chain) {
        return new SingleProcessorFilter(chain);
    }

    @Bean
    public ReactorContextCaptureAspect reactorContextCaptureAspect() {
        return new ReactorContextCaptureAspect();
    }


}
