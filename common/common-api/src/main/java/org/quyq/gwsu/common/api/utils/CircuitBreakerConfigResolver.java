package org.quyq.gwsu.common.api.utils;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.api.annotation.CircuitBreakerCustomConfig;
import org.quyq.gwsu.common.api.config.properties.CircuitBreakerProperties;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * @author Quyq
 * @date 2026/4/1
 * @description 熔断器配置解析工具类
 * 支持从注解和配置文件中解析熔断器配置，优先级：方法注解 > 类注解 > 配置文件
 */
public final class CircuitBreakerConfigResolver {

    private CircuitBreakerConfigResolver() {
    }

    /**
     * 解析熔断器配置
     * 优先级：方法注解配置 > 类注解配置 > 配置文件配置
     *
     * @param method            目标方法
     * @param defaultProperties 默认配置属性（来自配置文件）
     * @return 熔断器配置
     */
    public static CircuitBreakerConfig resolve(
            ApiClient apiClient, Method method, CircuitBreakerProperties defaultProperties) {
        CircuitBreakerCustomConfig methodConfig =
                method.getAnnotation(CircuitBreakerCustomConfig.class);
        CircuitBreakerCustomConfig classConfig = apiClient.config();

        return buildConfig(methodConfig, classConfig, defaultProperties);
    }

    /**
     * 构建熔断器配置
     *
     * @param methodConfig      方法级别注解配置
     * @param classConfig       类级别注解配置
     * @param defaultProperties 默认配置属性
     * @return 熔断器配置
     */
    private static CircuitBreakerConfig buildConfig(
            CircuitBreakerCustomConfig methodConfig,
            CircuitBreakerCustomConfig classConfig,
            CircuitBreakerProperties defaultProperties) {

        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom();

        builder.failureRateThreshold(
                resolveFailureRateThreshold(methodConfig, classConfig, defaultProperties)
        );

        builder.slowCallRateThreshold(
                resolveSlowCallRateThreshold(methodConfig, classConfig, defaultProperties)
        );

        builder.slowCallDurationThreshold(
                resolveSlowCallDurationThreshold(methodConfig, classConfig, defaultProperties)
        );

        builder.waitDurationInOpenState(
                resolveWaitDurationInOpenState(methodConfig, classConfig, defaultProperties)
        );

        builder.slidingWindowSize(
                resolveSlidingWindowSize(methodConfig, classConfig, defaultProperties)
        );

        builder.permittedNumberOfCallsInHalfOpenState(
                resolveHalfOpenStatePermittedNumberOfCalls(methodConfig, classConfig, defaultProperties)
        );

        return builder.build();
    }

    private static float resolveFailureRateThreshold(
            CircuitBreakerCustomConfig methodConfig,
            CircuitBreakerCustomConfig classConfig,
            CircuitBreakerProperties defaultProperties) {
        if (methodConfig != null && methodConfig.failureRateThreshold() > 0) {
            return methodConfig.failureRateThreshold();
        }
        if (classConfig.failureRateThreshold() > 0) {
            return classConfig.failureRateThreshold();
        }
        return defaultProperties.getFailureRateThreshold();
    }

    private static float resolveSlowCallRateThreshold(
            CircuitBreakerCustomConfig methodConfig,
            CircuitBreakerCustomConfig classConfig,
            CircuitBreakerProperties defaultProperties) {
        if (methodConfig != null && methodConfig.slowCallRateThreshold() > 0) {
            return methodConfig.slowCallRateThreshold();
        }
        if (classConfig.slowCallRateThreshold() > 0) {
            return classConfig.slowCallRateThreshold();
        }
        return defaultProperties.getSlowCallRateThreshold();
    }

    private static Duration resolveSlowCallDurationThreshold(
            CircuitBreakerCustomConfig methodConfig,
            CircuitBreakerCustomConfig classConfig,
            CircuitBreakerProperties defaultProperties) {
        if (methodConfig != null && methodConfig.slowCallDurationThreshold() > 0) {
            return Duration.ofSeconds(methodConfig.slowCallDurationThreshold());
        }
        if (classConfig.slowCallDurationThreshold() > 0) {
            return Duration.ofSeconds(classConfig.slowCallDurationThreshold());
        }
        return Duration.ofSeconds(defaultProperties.getSlowCallDurationThreshold());
    }

    private static Duration resolveWaitDurationInOpenState(
            CircuitBreakerCustomConfig methodConfig,
            CircuitBreakerCustomConfig classConfig,
            CircuitBreakerProperties defaultProperties) {
        if (methodConfig != null && methodConfig.waitDurationInOpenState() > 0) {
            return Duration.ofSeconds(methodConfig.waitDurationInOpenState());
        }
        if (classConfig.waitDurationInOpenState() > 0) {
            return Duration.ofSeconds(classConfig.waitDurationInOpenState());
        }
        return Duration.ofSeconds(defaultProperties.getWaitDurationInOpenState());
    }

    private static int resolveSlidingWindowSize(
            CircuitBreakerCustomConfig methodConfig,
            CircuitBreakerCustomConfig classConfig,
            CircuitBreakerProperties defaultProperties) {
        if (methodConfig != null && methodConfig.slidingWindowSize() > 0) {
            return methodConfig.slidingWindowSize();
        }
        if (classConfig.slidingWindowSize() > 0) {
            return classConfig.slidingWindowSize();
        }
        return defaultProperties.getSlidingWindowSize();
    }

    private static int resolveHalfOpenStatePermittedNumberOfCalls(
            CircuitBreakerCustomConfig methodConfig,
            CircuitBreakerCustomConfig classConfig,
            CircuitBreakerProperties defaultProperties) {
        if (methodConfig != null && methodConfig.halfOpenStatePermittedNumberOfCalls() > 0) {
            return methodConfig.halfOpenStatePermittedNumberOfCalls();
        }
        if (classConfig.halfOpenStatePermittedNumberOfCalls() > 0) {
            return classConfig.halfOpenStatePermittedNumberOfCalls();
        }
        return defaultProperties.getHalfOpenStatePermittedNumberOfCalls();
    }
}
