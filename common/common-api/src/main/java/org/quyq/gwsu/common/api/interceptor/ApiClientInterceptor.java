package org.quyq.gwsu.common.api.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import java.util.function.Consumer;

/**
 * API 客户端请求拦截器
 * <p>
 * 类似 Feign 的 RequestInterceptor，用于在请求发送前添加请求头或其他定制操作。
 * 使用方式：在 Spring 配置类中定义 @Bean
 * <pre>
 * &#64;Bean
 * public ApiClientInterceptor authInterceptor() {
 *     return (headers, serviceName) -> {
 *         headers.add("Authorization", "Bearer token");
 *     };
 * }
 * </pre>
 *
 * @author Quyq
 * @date 2026/4/12
 */
@FunctionalInterface
public interface ApiClientInterceptor {

    /**
     * 拦截请求并添加定制内容
     *
     * @param headers     请求头操作器，用于添加或修改请求头
     * @param serviceName 目标服务名称
     */
    void intercept(HttpHeaders headers, String serviceName);
}
