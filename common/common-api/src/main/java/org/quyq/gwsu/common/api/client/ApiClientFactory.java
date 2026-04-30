package org.quyq.gwsu.common.api.client;


import org.quyq.gwsu.common.api.annotation.ApiClient;

/**
 * @author Quyq
 * @date 2026/3/13
 * @description API 客户端工厂，用于创建 API 客户端代理
 */
public interface ApiClientFactory {

    /**
     * 创建 API 客户端代理
     *
     * @param apiClientClass API 客户端接口类
     * @param <T>            API 客户端类型
     * @return API 客户端代理实例
     */
    <T> T createClient(Class<T> apiClientClass);

    /**
     * 获取 API 客户端注解信息
     *
     * @param apiClientClass API 客户端接口类
     * @return ApiClient 注解
     */
    default ApiClient getApiClientAnnotation(Class<?> apiClientClass) {
        return apiClientClass.getAnnotation(ApiClient.class);
    }
}
