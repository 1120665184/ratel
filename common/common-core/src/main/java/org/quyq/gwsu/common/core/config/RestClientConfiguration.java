package org.quyq.gwsu.common.core.config;


import org.quyq.gwsu.common.core.interceptor.ApiClientInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@AutoConfiguration
public class RestClientConfiguration {


    @AutoConfiguration
    @ConditionalOnClass(DeferringLoadBalancerInterceptor.class)
    public static class RestClientLoadBalancerConfiguration {

        @Bean
        public RestClient.Builder loadBalancedRestClientBuilder(List<ApiClientInterceptor> interceptors,
                                                                ObjectProvider<DeferringLoadBalancerInterceptor> loadBalancerInterceptorProvider,
                                                                JsonMapper jsonMapper) {
            RestClient.Builder builder = RestClient.builder()
                    .configureMessageConverters(registry -> {
                        registry.registerDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper));
                    });
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
    public RestClient.Builder loadBalancedRestClientBuilder(List<ApiClientInterceptor> interceptors,
                                                            JsonMapper jsonMapper) {
        RestClient.Builder builder = RestClient.builder()
                .configureMessageConverters(registry -> {
                    registry.registerDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper));
                });
        builder.requestInterceptor(createRequestInterceptor(interceptors));
        return builder;
    }

    private static ClientHttpRequestInterceptor createRequestInterceptor(List<ApiClientInterceptor> interceptors) {
        return (request, body, execution) -> {
            HttpHeaders additionalHeaders = new HttpHeaders();
            for (ApiClientInterceptor interceptor : interceptors) {
                interceptor.intercept(additionalHeaders);
            }

            if (!additionalHeaders.isEmpty()) {
                request.getHeaders().putAll(additionalHeaders);
            }

            return execution.execute(request, body);
        };
    }

}
