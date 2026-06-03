package org.quyq.gwsu.common.core.config;


import org.quyq.gwsu.common.core.interceptor.ApiClientInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/6/3
 * @description
 */
@AutoConfiguration
public class RestClientConfiguration {


    @AutoConfiguration
    @ConditionalOnClass(DeferringLoadBalancerInterceptor.class)
    public static class RestClientLoadBalancerConfiguration {
        //支持负载均衡
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
    }

    @Bean
    @ConditionalOnMissingClass(value = "org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor")
    public RestClient.Builder loadBalancedRestClientBuilder(List<ApiClientInterceptor> interceptors) {
        RestClient.Builder builder = RestClient.builder();
        builder.requestInterceptor(createRequestInterceptor(interceptors));
        return builder;
    }





    /**
     * 创建 ClientHttpRequestInterceptor 来执行自定义拦截器
     *
     * @return ClientHttpRequestInterceptor 实例
     */
    private static ClientHttpRequestInterceptor createRequestInterceptor(List<ApiClientInterceptor> interceptors) {
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

}
