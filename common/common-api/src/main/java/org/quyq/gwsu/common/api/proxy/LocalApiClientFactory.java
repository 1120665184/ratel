package org.quyq.gwsu.common.api.proxy;

import org.quyq.gwsu.common.api.client.ApiClientFactory;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/3/13
 * @description 单应用模式本地调用代理工厂，直接查找 API 接口的实现类
 */
@Component
public class LocalApiClientFactory implements ApiClientFactory {

    @Override
    public <T> T createClient(Class<T> apiClientClass) {
        Object implementation = findImplementation(apiClientClass);
        if (implementation == null) {
            throw new IllegalStateException("找不到 API 接口的实现类: " + apiClientClass.getName());
        }
        return (T) implementation;
    }

    /**
     * 查找 API 接口的实现类
     *
     * @param apiClientClass API 接口类
     * @param <T>            接口类型
     * @return 实现类实例
     */
    private <T> T findImplementation(Class<T> apiClientClass) {
        try {
            return SpringUtils.getBean(apiClientClass);
        } catch (Exception e) {
            return null;
        }
    }
}
