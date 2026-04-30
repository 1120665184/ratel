package org.quyq.gwsu.common.api.client;


import org.quyq.gwsu.common.api.proxy.LocalApiClientFactory;
import org.quyq.gwsu.common.api.proxy.RemoteApiClientFactory;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/3/13
 * @description 统一的 API 客户端工厂，根据部署模式自动选择代理实现
 */
@Component
public class UnifiedApiClientFactory {

    private final LocalApiClientFactory localApiClientFactory;
    private final RemoteApiClientFactory remoteApiClientFactory;

    @Autowired
    public UnifiedApiClientFactory(
            LocalApiClientFactory localApiClientFactory,
            RemoteApiClientFactory remoteApiClientFactory) {
        this.localApiClientFactory = localApiClientFactory;
        this.remoteApiClientFactory = remoteApiClientFactory;
    }

    /**
     * 创建 API 客户端代理，根据部署模式自动选择实现
     *
     * @param apiClientClass API 客户端接口类
     * @param <T>            API 客户端类型
     * @return API 客户端代理实例
     */
    public <T> T createClient(Class<T> apiClientClass) {
        if (DeployUtils.isSingle()) {
            return localApiClientFactory.createClient(apiClientClass);
        } else {
            return remoteApiClientFactory.createClient(apiClientClass);
        }
    }
}
