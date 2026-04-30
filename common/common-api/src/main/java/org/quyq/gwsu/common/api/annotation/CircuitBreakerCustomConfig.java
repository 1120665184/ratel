package org.quyq.gwsu.common.api.annotation;

import java.lang.annotation.*;

/**
 * @author Quyq
 * @date 2026/4/1
 * @description 熔断器配置注解，用于在 API 接口类或方法上配置熔断器参数
 * 优先级：方法注解配置 > 类注解配置 > 配置文件配置
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CircuitBreakerCustomConfig {

    /**
     * 失败率阈值（百分比）
     * 当滑动窗口内的调用失败率达到此阈值时，熔断器将从关闭状态切换到开启状态
     * 取值范围：0.0 ~ 100.0
     * 默认值：-1.0f 表示使用配置文件的值
     *
     * @return 失败率阈值
     */
    float failureRateThreshold() default -1.0f;

    /**
     * 慢调用率阈值（百分比）
     * 当滑动窗口内的慢调用率达到此阈值时，熔断器将从关闭状态切换到开启状态
     * 取值范围：0.0 ~ 100.0
     * 默认值：-1.0f 表示使用配置文件的值
     *
     * @return 慢调用率阈值
     */
    float slowCallRateThreshold() default -1.0f;

    /**
     * 慢调用持续时间阈值（秒）
     * 调用耗时超过此阈值则被视为慢调用
     * 默认值：-1 表示使用配置文件的值
     *
     * @return 慢调用持续时间阈值（秒）
     */
    long slowCallDurationThreshold() default -1;

    /**
     * 熔断器在开启状态下的等待时间（秒）
     * 熔断器开启后，将等待此时间后进入半开状态，尝试探测服务是否恢复
     * 默认值：-1 表示使用配置文件的值
     *
     * @return 等待时间（秒）
     */
    long waitDurationInOpenState() default -1;

    /**
     * 滑动窗口大小
     * 用于计算失败率和慢调用率的调用次数窗口
     * 熔断器将基于最近的 N 次调用来计算失败率和慢调用率
     * 默认值：-1 表示使用配置文件的值
     *
     * @return 滑动窗口大小
     */
    int slidingWindowSize() default -1;

    /**
     * 半开状态下允许的调用次数
     * 熔断器从开启状态进入半开状态后，将允许指定数量的调用通过
     * 如果这些调用成功，熔断器将切换回关闭状态；如果失败，则重新进入开启状态
     * 默认值：-1 表示使用配置文件的值
     *
     * @return 半开状态下允许的调用次数
     */
    int halfOpenStatePermittedNumberOfCalls() default -1;
}
