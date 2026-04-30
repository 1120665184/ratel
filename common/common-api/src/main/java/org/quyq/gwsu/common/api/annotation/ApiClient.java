package org.quyq.gwsu.common.api.annotation;


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
}
