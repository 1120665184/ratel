package org.quyq.gwsu.common.api.fallback;

/**
 * @author Quyq
 * @date 2026/3/27
 * @description 降级工厂接口，类似 OpenFeign 的 FallbackFactory
 * @param <T> API 客户端类型
 */
public interface FallbackFactory<T> {

    /**
     * 创建降级实例
     *
     * @param cause 导致降级的原因（异常信息）
     * @return 降级实例
     */
    T create(Throwable cause);
}