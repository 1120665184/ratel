package org.quyq.gwsu.common.api.interceptor;

import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * API 客户端 WebClient 请求过滤器
 * <p>
 * 用于在 WebClient 请求发送前修改请求头或其他定制操作。
 * 类似于 {@link org.quyq.gwsu.common.core.interceptor.ApiClientInterceptor}，但作用于 WebClient 而非 RestClient。
 *
 * @author Quyq
 * @date 2026/6/25
 */
@FunctionalInterface
public interface ApiClientWebClientFilter {

    /**
     * 过滤请求
     *
     * @param requestBuilder 请求构建器，用于修改请求头等
     */
    void filter(ClientRequest.Builder requestBuilder);
}
