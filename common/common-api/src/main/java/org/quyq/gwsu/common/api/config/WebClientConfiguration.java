package org.quyq.gwsu.common.api.config;

import io.netty.channel.ChannelOption;
import org.quyq.gwsu.common.api.interceptor.ApiClientWebClientFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

/**
 * WebClient 自动配置，提供带负载均衡和请求头传播的 WebClient.Builder
 *
 * @author Quyq
 * @date 2026/6/25
 */
@AutoConfiguration
public class WebClientConfiguration {

    /**
     * 分布式模式：带 Spring Cloud LoadBalancer 的 WebClient.Builder
     */
    @AutoConfiguration
    @ConditionalOnClass(DeferringLoadBalancerInterceptor.class)
    public static class WebClientLoadBalancerConfiguration {

        @Bean
        public WebClient.Builder loadBalancedWebClientBuilder(
                List<ApiClientWebClientFilter> filters,
                ObjectProvider<ReactorLoadBalancerExchangeFilterFunction> loadBalancerFilterProvider,
                ObjectProvider<HttpClient> httpClientProvider) {

            WebClient.Builder builder = createWebClientBuilder(filters, httpClientProvider);

            ReactorLoadBalancerExchangeFilterFunction lbFilter = loadBalancerFilterProvider.getIfAvailable();
            if (lbFilter != null) {
                builder.filter(lbFilter);
            }

            return builder;
        }
    }

    /**
     * 单点/无负载均衡模式：不带 LoadBalancer 的 WebClient.Builder
     */
    @Bean
    @ConditionalOnMissingClass(value = "org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor")
    public WebClient.Builder loadBalancedWebClientBuilder(
            List<ApiClientWebClientFilter> filters,
            ObjectProvider<HttpClient> httpClientProvider) {

        return createWebClientBuilder(filters, httpClientProvider);
    }

    /**
     * 创建 WebClient.Builder 基础配置
     */
    private static WebClient.Builder createWebClientBuilder(
            List<ApiClientWebClientFilter> filters,
            ObjectProvider<HttpClient> httpClientProvider) {

        HttpClient httpClient = httpClientProvider.getIfAvailable(() ->
                HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(30))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        );

        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (!filters.isEmpty()) {
            builder.filter(createFilterFunction(filters));
        }

        return builder;
    }

    /**
     * 将 ApiClientWebClientFilter 列表转换为 ExchangeFilterFunction
     */
    private static ExchangeFilterFunction createFilterFunction(List<ApiClientWebClientFilter> filters) {
        return (request, next) -> {
            ClientRequest.Builder requestBuilder = ClientRequest.from(request);
            for (ApiClientWebClientFilter filter : filters) {
                filter.filter(requestBuilder);
            }
            return next.exchange(requestBuilder.build());
        };
    }
}
