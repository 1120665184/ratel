package org.quyq.gwsu.common.api.proxy;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.api.annotation.CircuitBreakerCustomConfig;
import org.quyq.gwsu.common.api.client.ApiClientFactory;
import org.quyq.gwsu.common.api.config.properties.CircuitBreakerProperties;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.api.loadbalancer.ServiceInstanceLoadBalancerFilterFunction;
import org.quyq.gwsu.common.api.resolver.MultipartDtoArgumentResolver;
import org.quyq.gwsu.common.api.utils.CircuitBreakerConfigResolver;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分布式模式 HTTP 调用代理工厂，使用 WebClient 统一支持同步和响应式调用，支持熔断降级和自定义负载均衡
 *
 * @author Quyq
 * @date 2026/3/13
 */
public class RemoteApiClientFactory implements ApiClientFactory {

    private final WebClient.Builder webClientBuilder;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<String, CircuitBreaker> circuitBreakerCache = new ConcurrentHashMap<>();
    private final CircuitBreakerProperties circuitBreakerProperties;
    private final ObjectProvider<ReactorLoadBalancerExchangeFilterFunction> defaultLbFilterProvider;
    private final ObjectProvider<LoadBalancerClientFactory> lbClientFactoryProvider;

    public RemoteApiClientFactory(WebClient.Builder webClientBuilder,
                                  CircuitBreakerProperties circuitBreakerProperties,
                                  ObjectProvider<ReactorLoadBalancerExchangeFilterFunction> defaultLbFilterProvider,
                                  ObjectProvider<LoadBalancerClientFactory> lbClientFactoryProvider) {
        this.webClientBuilder = webClientBuilder;
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(createDefaultCircuitBreakerConfig());
        this.defaultLbFilterProvider = defaultLbFilterProvider;
        this.lbClientFactoryProvider = lbClientFactoryProvider;
    }

    @Override
    public <T> T createClient(Class<T> apiClientClass) {
        ApiClient apiClient = getApiClientAnnotation(apiClientClass);

        String serviceName = apiClient.value();
        Class<?> loadBalancerClass = apiClient.loadBalancer();

        WebClient webClient = createWebClient(serviceName, loadBalancerClass);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                        WebClientAdapter.create(webClient))
                .customArgumentResolver(new MultipartDtoArgumentResolver())
                .build();

        T client = factory.createClient(apiClientClass);

        Class<?> fallbackFactoryClass = apiClient.fallbackFactory();
        if (fallbackFactoryClass != Void.class) {
            return createCircuitBreakerProxy(apiClientClass, client, fallbackFactoryClass, serviceName);
        }

        return client;
    }

    /**
     * 创建 WebClient 实例，根据 @ApiClient.loadBalancer 属性注入对应的负载均衡策略
     *
     * @param serviceName       服务名称
     * @param loadBalancerClass 负载均衡策略实现类，Void.class 表示使用全局默认
     * @return WebClient 实例
     */
    private WebClient createWebClient(String serviceName, Class<?> loadBalancerClass) {
        WebClient.Builder builder = webClientBuilder.clone()
                .baseUrl("http://%s".formatted(serviceName));

        if (loadBalancerClass != ApiClient.NullLoadBalancer.class) {
            // 自定义负载均衡策略
            ReactorServiceInstanceLoadBalancer lb = createLoadBalancer(loadBalancerClass, serviceName);
            builder.filter(new ServiceInstanceLoadBalancerFilterFunction(lb));
        } else {
            // 默认负载均衡策略（共享 Bean）
            ReactorLoadBalancerExchangeFilterFunction defaultLb = defaultLbFilterProvider.getIfAvailable();
            if (defaultLb != null) {
                builder.filter(defaultLb);
            }
        }

        return builder.build();
    }

    /**
     * 根据指定的负载均衡策略类创建实例
     * <p>
     * 策略类需提供 {@code (ObjectProvider<ServiceInstanceListSupplier>, String)} 构造函数，
     * 这也是 Spring Cloud 内置的 {@link org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer}、
     * {@link org.springframework.cloud.loadbalancer.core.RandomLoadBalancer} 等标准实现所采用的构造签名。
     *
     * @param loadBalancerClass 负载均衡策略类
     * @param serviceId         服务 ID
     * @return 负载均衡器实例
     */
    private ReactorServiceInstanceLoadBalancer createLoadBalancer(Class<?> loadBalancerClass, String serviceId) {
        LoadBalancerClientFactory factory = lbClientFactoryProvider.getIfAvailable();
        if (factory == null) {
            throw new IllegalStateException("LoadBalancerClientFactory 不可用，无法创建自定义负载均衡器: " + loadBalancerClass.getName());
        }

        ObjectProvider<ServiceInstanceListSupplier> supplier =
                factory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class);

        try {
            Constructor<?> ctor = loadBalancerClass.getConstructor(ObjectProvider.class, String.class);
            return (ReactorServiceInstanceLoadBalancer) ctor.newInstance(supplier, serviceId);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "负载均衡策略类 %s 缺少所需构造函数: (ObjectProvider<ServiceInstanceListSupplier>, String)"
                            .formatted(loadBalancerClass.getName()), e);
        } catch (Exception e) {
            throw new RuntimeException("无法创建负载均衡器实例: " + loadBalancerClass.getName(), e);
        }
    }

    /**
     * 创建带熔断器的代理，按方法返回类型分流处理
     */
    @SuppressWarnings("unchecked")
    private <T> T createCircuitBreakerProxy(Class<T> apiClientClass, T client, Class<?> fallbackFactoryClass, String serviceName) {
        FallbackFactory<T> fallbackFactory = createFallbackFactory(fallbackFactoryClass);

        return (T) Proxy.newProxyInstance(
                apiClientClass.getClassLoader(),
                new Class<?>[]{apiClientClass},
                (proxy, method, args) -> {
                    CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(apiClientClass, method);
                    Class<?> returnType = method.getReturnType();

                    if (isReactiveType(returnType)) {
                        return invokeReactiveWithCircuitBreaker(client, method, args, circuitBreaker, fallbackFactory, returnType);
                    } else {
                        return invokeSyncWithCircuitBreaker(client, method, args, circuitBreaker, fallbackFactory);
                    }
                }
        );
    }

    /**
     * 同步调用路径：使用 decorateCallable 包装
     */
    private Object invokeSyncWithCircuitBreaker(Object client, Method method, Object[] args,
                                                CircuitBreaker circuitBreaker, FallbackFactory<?> fallbackFactory) {
        try {
            return CircuitBreaker.decorateCallable(circuitBreaker, () -> method.invoke(client, args)).call();
        } catch (Throwable throwable) {
            Throwable error = throwable;
            if (error instanceof InvocationTargetException invocationTargetException) {
                error = invocationTargetException.getCause();
            }
            Object fallbackInstance = fallbackFactory.create(error);
            try {
                return method.invoke(fallbackInstance, args);
            } catch (Exception e) {
                throw new RuntimeException("降级处理失败", e);
            }
        }
    }

    /**
     * 响应式调用路径：使用 CircuitBreakerOperator 包装
     */
    private Object invokeReactiveWithCircuitBreaker(Object client, Method method, Object[] args,
                                                    CircuitBreaker circuitBreaker, FallbackFactory<?> fallbackFactory,
                                                    Class<?> returnType) {

        if (Flux.class.isAssignableFrom(returnType)) {
            return invokeFluxWithCircuitBreaker(client, method, args, circuitBreaker, fallbackFactory);
        } else {
            return invokeMonoWithCircuitBreaker(client, method, args, circuitBreaker, fallbackFactory);
        }
    }

    /**
     * Flux 路径熔断包装
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object invokeFluxWithCircuitBreaker(Object client, Method method, Object[] args,
                                                CircuitBreaker circuitBreaker, FallbackFactory<?> fallbackFactory) {
        return Mono.fromCallable(() -> invokeMethod(client, method, args))
                .flatMapMany(result -> (Flux) result)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(error -> {
                    Object fallback = fallbackFactory.create((Throwable) error);
                    try {
                        return (Flux) method.invoke(fallback, args);
                    } catch (Exception e) {
                        return Flux.error(e);
                    }
                });
    }

    /**
     * Mono 路径熔断包装
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object invokeMonoWithCircuitBreaker(Object client, Method method, Object[] args,
                                                CircuitBreaker circuitBreaker, FallbackFactory<?> fallbackFactory) {
        return Mono.fromCallable(() -> invokeMethod(client, method, args))
                .flatMap(result -> (Mono) result)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(error -> {
                    Object fallback = fallbackFactory.create((Throwable) error);
                    try {
                        return (Mono) method.invoke(fallback, args);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    /**
     * 反射调用方法，解包 InvocationTargetException
     */
    private Object invokeMethod(Object target, Method method, Object[] args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw cause instanceof RuntimeException re ? re : new RuntimeException(cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("方法访问失败: " + method.getName(), e);
        }
    }

    /**
     * 判断返回类型是否为响应式类型
     */
    private boolean isReactiveType(Class<?> returnType) {
        return Flux.class.isAssignableFrom(returnType) || Mono.class.isAssignableFrom(returnType);
    }

    /**
     * 获取或创建熔断器实例
     * 支持方法级别的熔断器配置，优先级：方法注解 > 类注解 > 配置文件
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
