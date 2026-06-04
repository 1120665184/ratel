package org.quyq.gwsu.common.deploy.config;


import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.core.utils.filter.ProcessorChain;
import org.quyq.gwsu.common.deploy.aop.ReactorContextCaptureAspect;
import org.quyq.gwsu.common.deploy.controller.DistributedSqlExecutionController;
import org.quyq.gwsu.common.deploy.domain.ApplicationModules;
import org.quyq.gwsu.common.deploy.filter.DistributedGatewayProcessorFilter;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author Quyq
 * @date 2026/3/11
 * @description 分布式模式部署时触发
 */
@AutoConfiguration
@AutoConfigureBefore(NacosDiscoveryAutoConfiguration.class)
@ConditionalOnProperty(name = CoreConstants.Yaml.DEPLOY_SINGLE, havingValue = "false")
public class DeployDistributedConfiguration {


    /**
     * 将服务前缀在向注册中心注册时添加到metadata中
     *
     * @param provider
     * @return
     */
    @Bean
    @ConditionalOnClass(NacosDiscoveryProperties.class)
    public NacosDiscoveryProperties nacosProperties(ObjectProvider<BusinessModuleInfoProvider> provider) {
        NacosDiscoveryProperties properties = new NacosDiscoveryProperties();
        Optional.ofNullable(provider.getIfAvailable())
                .ifPresent(p -> {
                    properties.getMetadata().put("prefix", p.module().prefix());
                    properties.getMetadata().put("note", p.module().note());
                });
        return properties;

    }


    @AutoConfiguration
    @ConditionalOnMissingClass({"org.springframework.cloud.gateway.config.GatewayAutoConfiguration"})
    public static class DistributeBusinessServerConfiguration {
        /**
         * 微服务模式时，各服务添加执行sql接口
         *
         * @param sqlExecutionService
         * @return
         */
        @Bean
        public DistributedSqlExecutionController distributedSqlExecutionController(ISQLExecutionService sqlExecutionService) {
            return new DistributedSqlExecutionController(sqlExecutionService);
        }

        @Bean
        public ReactorContextCaptureAspect reactorContextCaptureAspect() {
            return new ReactorContextCaptureAspect();
        }
    }

    @AutoConfiguration
    @ConditionalOnClass(GatewayAutoConfiguration.class)
    public static class DistributedGatewayConfiguration {


        @Bean
        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        public RouterFunction<ServerResponse> moulesRoutes(ReactiveDiscoveryClient discoveryClient) {
            RouterFunctions.Builder route = RouterFunctions.route();

            route.POST(CoreConstants.EndPoint.ENDPOINT_MODULE_INFOS,
                    _ -> getModulesList(discoveryClient));

            return route.build();
        }

        @Bean
        public DistributedGatewayProcessorFilter distributedGatewayProcessorFilter(ProcessorChain chain) {
            return new DistributedGatewayProcessorFilter(chain);

        }


        /**
         * 通过注册的服务获取所有模块信息
         *
         * @param discoveryClient
         * @return
         */
        private Mono<ServerResponse> getModulesList(ReactiveDiscoveryClient discoveryClient) {

            SimpleEvaluationContext evalCtxt = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods().build();

            Expression includeExpr = new SpelExpressionParser().parseExpression("metadata['prefix'] != null && metadata['prefix'] != ''");

            Predicate<ServiceInstance> includePredicate = instance -> {
                Boolean include = includeExpr.getValue(evalCtxt, instance, Boolean.class);
                if (include == null) {
                    return false;
                }
                return include;
            };


            return discoveryClient.getServices()
                    .flatMap(service -> discoveryClient.getInstances(service).collectList())
                    .flatMap(Flux::fromIterable)
                    .filter(includePredicate)
                    .collectMap(ServiceInstance::getServiceId)
                    .flatMapMany(map -> Flux.fromIterable(map.values()))
                    .collectList()
                    .flatMap(instances -> {

                        Map<BusinessModuleInfo, String> moduleInfos = new HashMap<>();
                        instances.forEach(instance ->
                                moduleInfos.put(new BusinessModuleInfo(
                                        Optional.ofNullable(instance.getMetadata()).map(v -> v.get("prefix")).orElse(null),
                                        Optional.ofNullable(instance.getMetadata()).map(v -> v.get("note")).orElse(null)), instance.getServiceId())
                        );

                        return ServerResponse.ok().bodyValue(R.ok(ApplicationModules.transformationModules(moduleInfos)));
                    })
                    .switchIfEmpty(ServerResponse.ok().bodyValue(R.ok(Collections.emptyList())));

        }


    }


}
