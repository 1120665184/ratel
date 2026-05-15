package org.quyq.gwsu.common.api.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.api.annotation.CircuitBreakerCustomConfig;
import org.quyq.gwsu.common.api.client.ApiClientFactory;
import org.quyq.gwsu.common.api.config.properties.CircuitBreakerProperties;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.api.utils.CircuitBreakerConfigResolver;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Quyq
 * @date 2026/3/13
 * @description 分布式模式 HTTP 调用代理工厂，支持熔断降级和请求拦截器
 */
@Component
public class RemoteApiClientFactory implements ApiClientFactory {

    private final RestClient.Builder restClientBuilder;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<String, CircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();
    private final CircuitBreakerProperties circuitBreakerProperties;

    public RemoteApiClientFactory(RestClient.Builder restClientBuilder,
                                  CircuitBreakerProperties circuitBreakerProperties) {
        this.restClientBuilder = restClientBuilder;
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(createDefaultCircuitBreakerConfig());
    }

    @Override
    public <T> T createClient(Class<T> apiClientClass) {
        ApiClient apiClient = getApiClientAnnotation(apiClientClass);

        String serviceName = apiClient.value();

        // 创建带有拦截器支持的 RestClient
        RestClient restClient = createRestClient(serviceName);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(restClient)).build();

        T client = factory.createClient(apiClientClass);

        Class<?> fallbackFactoryClass = apiClient.fallbackFactory();
        if (fallbackFactoryClass != Void.class) {
            return createCircuitBreakerProxy(apiClientClass, client, fallbackFactoryClass, serviceName);
        }

        return client;
    }

    /**
     * 创建带有拦截器支持的 RestClient
     *
     * @param serviceName 服务名称
     * @return RestClient 实例
     */
    private RestClient createRestClient(String serviceName) {
        RestClient.Builder builder = restClientBuilder.clone()
                .baseUrl("http://%s".formatted(serviceName));

        return builder.build();
    }


    /**
     * 创建带熔断器的代理
     *
     * @param apiClientClass       API 客户端接口类
     * @param client               原始客户端
     * @param fallbackFactoryClass 降级工厂类
     * @param serviceName          服务名称
     * @param <T>                  客户端类型
     * @return 带熔断器的代理
     */
    @SuppressWarnings("unchecked")
    private <T> T createCircuitBreakerProxy(Class<T> apiClientClass, T client, Class<?> fallbackFactoryClass, String serviceName) {
        FallbackFactory<T> fallbackFactory = createFallbackFactory(fallbackFactoryClass);

        return (T) Proxy.newProxyInstance(
                apiClientClass.getClassLoader(),
                new Class<?>[]{apiClientClass},
                (proxy, method, args) -> {
                    CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(apiClientClass, method);
                    try {
                        return CircuitBreaker.decorateCallable(circuitBreaker, () -> method.invoke(client, args)).call();
                    } catch (Throwable throwable) {
                        Throwable error = throwable;
                        if (error instanceof InvocationTargetException invocationTargetException) {
                            error = invocationTargetException.getCause();
                        }
                        T fallbackInstance = fallbackFactory.create(error);
                        try {
                            return method.invoke(fallbackInstance, args);
                        } catch (Exception e) {
                            throw new RuntimeException("降级处理失败", e);
                        }
                    }
                }
        );
    }

    /**
     * 获取或创建熔断器实例
     * 支持方法级别的熔断器配置，优先级：方法注解 > 类注解 > 配置文件
     *
     * @param apiClientClass API 客户端接口类
     * @param method         目标方法
     * @return 熔断器实例
     */
    private CircuitBreaker getOrCreateCircuitBreaker(Class<?> apiClientClass, Method method) {
        String circuitBreakerName = generateCircuitBreakerName(apiClientClass, method);

        return circuitBreakerCache.computeIfAbsent(circuitBreakerName, name -> {
            CircuitBreakerConfig config = CircuitBreakerConfigResolver.resolve(getApiClientAnnotation(apiClientClass), method, circuitBreakerProperties);
            return circuitBreakerRegistry.circuitBreaker(name, config);
        });
    }

    /**
     * 生成熔断器名称
     * 如果方法上有 @CircuitBreakerConfig 注解，则使用方法名作为后缀
     * 否则使用类名
     *
     * @param apiClientClass API 客户端接口类
     * @param method         目标方法
     * @return 熔断器名称
     */
    private String generateCircuitBreakerName(Class<?> apiClientClass, Method method) {
        CircuitBreakerCustomConfig methodConfig =
                method.getAnnotation(CircuitBreakerCustomConfig.class);

        if (methodConfig != null) {
            return apiClientClass.getName() + "#" + method.getName();
        }

        return apiClientClass.getName();
    }

    /**
     * 创建降级工厂实例
     *
     * @param fallbackFactoryClass 降级工厂类
     * @param <T>                  客户端类型
     * @return 降级工厂实例
     */
    @SuppressWarnings("unchecked")
    private <T> FallbackFactory<T> createFallbackFactory(Class<?> fallbackFactoryClass) {
        try {
            return (FallbackFactory<T>) SpringUtils.getBean(fallbackFactoryClass);
        } catch (Exception e) {
            try {
                return (FallbackFactory<T>) fallbackFactoryClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("无法创建降级工厂实例: " + fallbackFactoryClass.getName(), ex);
            }
        }
    }

    /**
     * 创建默认熔断器配置（来自配置文件）
     *
     * @return 熔断器配置
     */
    private CircuitBreakerConfig createDefaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(circuitBreakerProperties.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofSeconds(circuitBreakerProperties.getWaitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(circuitBreakerProperties.getHalfOpenStatePermittedNumberOfCalls())
                .slidingWindowSize(circuitBreakerProperties.getSlidingWindowSize())
                .slowCallRateThreshold(circuitBreakerProperties.getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofSeconds(circuitBreakerProperties.getSlowCallDurationThreshold()))
                .build();
    }
}
