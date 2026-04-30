package org.quyq.gwsu.common.api.config;


import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * @author Quyq
 * @date 2026/3/15
 * @description Spring Cloud LoadBalancer AOT 支持
 */
public class SpringCloudLoadBalancerHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerType(TypeReference.of("org.springframework.cloud.context.named.NamedContextFactory"), MemberCategory.values())
                .registerType(TypeReference.of("org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory"),
                        MemberCategory.values())
                .registerType(TypeReference.of("org.springframework.context.annotation.AnnotationConfigApplicationContext"),
                        MemberCategory.values())
                .registerType(TypeReference.of("org.springframework.context.support.GenericApplicationContext"),
                        MemberCategory.values());

        hints.resources()
                .registerPattern("org/springframework/cloud/loadbalancer/**")
                .registerPattern("META-INF/spring/**");
    }
}
