package org.quyq.gwsu.common.api.config;

import org.quyq.gwsu.common.api.interceptor.ApiClientWebClientFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * WebClient 自动配置，提供带 Jackson 编解码和请求头传播的 WebClient.Builder
 * <p>
 * 负载均衡不在此处配置，而是由 {@link org.quyq.gwsu.common.api.proxy.RemoteApiClientFactory}
 * 根据 {@link org.quyq.gwsu.common.api.annotation.ApiClient#loadBalancer()} 属性按客户端注入。
 *
 * @author Quyq
 * @date 2026/6/25
 */
@AutoConfiguration
public class WebClientConfiguration {

    /**
     * WebClient.Builder 基础配置
     * <ul>
     *   <li>使用容器中的 JsonMapper 配置 Jackson 编解码器</li>
     *   <li>优先使用容器中的 ClientHttpConnector（Spring Boot 自动配置提供，含超时设置）</li>
     *   <li>注册请求头传播过滤器</li>
     * </ul>
     */
    @Bean
    public WebClient.Builder loadBalancedWebClientBuilder(
            List<ApiClientWebClientFilter> filters,
            ObjectProvider<ClientHttpConnector> clientHttpConnectorProvider,
            JsonMapper jsonMapper) {

        WebClient.Builder builder = WebClient.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
                    configurer.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
                });

        ClientHttpConnector connector = clientHttpConnectorProvider.getIfAvailable();
        if (connector != null) {
            builder.clientConnector(connector);
        }

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
