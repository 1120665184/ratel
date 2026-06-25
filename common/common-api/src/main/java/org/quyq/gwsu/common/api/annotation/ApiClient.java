package org.quyq.gwsu.common.api.annotation;


import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import reactor.core.publisher.Mono;

import java.lang.annotation.*;

/**
 * @author Quyq
 * @date 2026/3/13
 * @description 标记 API 客户端接口，类似 @FeignClient
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiClient {

    /**
     * 服务名称
     *
     * @return 服务名称
     */
    String value();

    /**
     * 模块描述
     *
     * @return 模块描述
     */
    String note() default "";

    /**
     * 熔断自定义配置
     *
     * @return
     */
    CircuitBreakerCustomConfig config() default @CircuitBreakerCustomConfig;

    /**
     * 降级工厂类，用于配置熔断降级实现
     * 工厂类需继承 FallbackFactory接口
     *
     * @return 降级工厂类
     */
    Class<?> fallbackFactory() default Void.class;

    /**
     * 自定义负载均衡策略
     * <p>
     * 指定 {@link org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer} 的实现类，
     * 如 {@link org.springframework.cloud.loadbalancer.core.RandomLoadBalancer}、
     * {@link org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer} 等。
     * <p>
     * 默认 {@code Void.class} 表示使用全局默认负载均衡策略。
     * 仅在分布式模式下生效，单点模式下此属性被忽略。
     *
     * @return 负载均衡策略实现类
     */
    Class<? extends ReactorServiceInstanceLoadBalancer> loadBalancer() default NullLoadBalancer.class;


    class NullLoadBalancer implements ReactorServiceInstanceLoadBalancer {
        @Override
        public Mono<Response<ServiceInstance>> choose(Request request) {
            return null;
        }
    }
}
