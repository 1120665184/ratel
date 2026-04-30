package org.quyq.gwsu.common.api.config.properties;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * @author Quyq
 * @date 2026/3/27
 * @description 熔断器配置属性
 */
@Data
@ConfigurationProperties(prefix = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".circuit-breaker")
public class CircuitBreakerProperties {

    /**
     * 失败率阈值（百分比）
     * 当滑动窗口内的调用失败率达到此阈值时，熔断器将从关闭状态切换到开启状态
     * 取值范围：0.0 ~ 100.0
     * 默认值：50.0（即 50% 失败率触发熔断）
     */
    private float failureRateThreshold = 50f;

    /**
     * 慢调用率阈值（百分比）
     * 当滑动窗口内的慢调用率达到此阈值时，熔断器将从关闭状态切换到开启状态
     * 取值范围：0.0 ~ 100.0
     * 默认值：50.0（即 50% 慢调用率触发熔断）
     */
    private float slowCallRateThreshold = 50f;

    /**
     * 慢调用持续时间阈值
     * 调用耗时超过此阈值则被视为慢调用
     * 默认值：5秒
     */
    private int slowCallDurationThreshold = 5;

    /**
     * 熔断器在开启状态下的等待时间
     * 熔断器开启后，将等待此时间后进入半开状态，尝试探测服务是否恢复
     * 默认值：30秒
     */
    private int waitDurationInOpenState = 30;

    /**
     * 滑动窗口大小
     * 用于计算失败率和慢调用率的调用次数窗口
     * 熔断器将基于最近的 N 次调用来计算失败率和慢调用率
     * 默认值：20（基于最近 20 次调用计算）
     */
    private int slidingWindowSize = 20;

    /**
     * 半开状态下允许的调用次数
     * 熔断器从开启状态进入半开状态后，将允许指定数量的调用通过
     * 如果这些调用成功，熔断器将切换回关闭状态；如果失败，则重新进入开启状态
     * 默认值：5（允许 5 次调用用于探测服务恢复情况）
     */
    private int halfOpenStatePermittedNumberOfCalls = 5;

}
