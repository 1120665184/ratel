package org.quyq.gwsu.common.api.config;


import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.api.client.UnifiedApiClientFactory;
import org.quyq.gwsu.common.api.config.properties.CircuitBreakerProperties;
import org.quyq.gwsu.common.api.interceptor.ApiClientInterceptor;
import org.quyq.gwsu.common.api.proxy.LocalApiClientFactory;
import org.quyq.gwsu.common.api.proxy.RemoteApiClientFactory;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

/**
 * @author Quyq
 * @date 2026/3/13
 * @description API 客户端自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@Import(ApiClientAutoConfiguration.ApiClientRegistrar.class)
@ImportRuntimeHints({ApiClientRuntimeHintsRegistrar.class, SpringCloudLoadBalancerHintsRegistrar.class})
public class ApiClientAutoConfiguration {

    /**
     * 配置负载均衡的 RestClient.Builder
     * <p>
     * 直接注入 DeferringLoadBalancerInterceptor 而非依赖 @LoadBalanced + BeanPostProcessor，
     * 因为 BeanPostProcessor 的注册时机可能晚于本配置类中 Bean 的实例化，导致拦截器未注入。
     * <p>
     * 注意：必须只注入 DeferringLoadBalancerInterceptor 一个负载均衡拦截器。
     * 不能注入 List&lt;ClientHttpRequestInterceptor&gt;，因为其中同时包含
     * LoadBalancerInterceptor 和 DeferringLoadBalancerInterceptor，会导致服务名被解析两次，
     * 第二次会把 IP 地址当作服务名解析而报错。
     */
    @Bean
    public RestClient.Builder loadBalancedRestClientBuilder(List<ApiClientInterceptor> interceptors,
                                                            ObjectProvider<DeferringLoadBalancerInterceptor> loadBalancerInterceptorProvider) {
        RestClient.Builder builder = RestClient.builder();
        builder.requestInterceptor(createRequestInterceptor(interceptors));
        DeferringLoadBalancerInterceptor interceptor = loadBalancerInterceptorProvider.getIfAvailable();
        if (interceptor != null) {
            builder.requestInterceptor(interceptor);
        }
        return builder;
    }


    @Bean
    @ConditionalOnMissingBean
    public LocalApiClientFactory localApiClientFactory() {
        return new LocalApiClientFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public RemoteApiClientFactory remoteApiClientFactory(
            @Lazy RestClient.Builder restClientBuilder,
            CircuitBreakerProperties circuitBreakerProperties) {
        return new RemoteApiClientFactory(restClientBuilder, circuitBreakerProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public UnifiedApiClientFactory unifiedApiClientFactory(
            LocalApiClientFactory localApiClientFactory,
            RemoteApiClientFactory remoteApiClientFactory) {
        return new UnifiedApiClientFactory(localApiClientFactory, remoteApiClientFactory);
    }


    /**
     * 创建 ClientHttpRequestInterceptor 来执行自定义拦截器
     *
     * @return ClientHttpRequestInterceptor 实例
     */
    private ClientHttpRequestInterceptor createRequestInterceptor(List<ApiClientInterceptor> interceptors) {
        return (request, body, execution) -> {
            // 创建新的 HttpHeaders 用于收集拦截器添加的头
            HttpHeaders additionalHeaders = new HttpHeaders();
            for (ApiClientInterceptor interceptor : interceptors) {
                interceptor.intercept(additionalHeaders);
            }

            // 将额外的请求头添加到原始请求中
            if (!additionalHeaders.isEmpty()) {
                request.getHeaders().putAll(additionalHeaders);
            }

            return execution.execute(request, body);
        };
    }

    static class ApiClientRegistrar implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    return beanDefinition.getMetadata().isInterface() &&
                            beanDefinition.getMetadata().isIndependent();
                }
            };

            scanner.addIncludeFilter(new AnnotationTypeFilter(ApiClient.class));

            Set<BeanDefinition> candidates = scanner.findCandidateComponents(CoreConstants.Project.COMMON_PACKAGE);

            for (BeanDefinition candidate : candidates) {
                String className = candidate.getBeanClassName();

                if (hasBeanForInterface(registry, className)) {
                    continue;
                }

                BeanDefinition beanDefinition = BeanDefinitionBuilder
                        .genericBeanDefinition(ApiClientFactoryBean.class)
                        .addConstructorArgValue(className)
                        .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
                        .getBeanDefinition();

                registry.registerBeanDefinition(className, beanDefinition);
            }
        }

        private boolean hasBeanForInterface(BeanDefinitionRegistry registry, String interfaceClassName) {
            if (!(registry instanceof ConfigurableListableBeanFactory)) {
                return false;
            }

            try {
                ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
                Class<?> interfaceClass = Class.forName(interfaceClassName);
                String[] beanNames = beanFactory.getBeanNamesForType(interfaceClass);
                return beanNames.length > 0;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }

    static class ApiClientFactoryBean<T> implements FactoryBean<T>,
            BeanFactoryAware, InitializingBean {

        private String className;
        private T instance;
        private BeanFactory beanFactory;

        public ApiClientFactoryBean(String className) {
            this.className = className;
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public void afterPropertiesSet() {
            try {
                Class<?> clazz = Class.forName(className);
                UnifiedApiClientFactory factory = beanFactory.getBean(UnifiedApiClientFactory.class);
                instance = (T) factory.createClient(clazz);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create API client for " + className, e);
            }
        }

        @Override
        public T getObject() {
            return instance;
        }

        @Override
        public Class<?> getObjectType() {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }
}
