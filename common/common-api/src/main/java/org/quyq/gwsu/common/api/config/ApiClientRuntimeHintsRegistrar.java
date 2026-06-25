package org.quyq.gwsu.common.api.config;


import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * @author Quyq
 * @date 2026/3/13
 * @description API 客户端运行时提示注册器，用于 AOT 编译
 */
public class ApiClientRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface() &&
                        beanDefinition.getMetadata().isIndependent();
            }
        };

        scanner.addIncludeFilter(new AnnotationTypeFilter(ApiClient.class));

        Set<org.springframework.beans.factory.config.BeanDefinition> candidates = scanner.findCandidateComponents(CoreConstants.Project.COMMON_PACKAGE);

        for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
            String className = candidate.getBeanClassName();
            try {
                Class<?> apiClientClass = Class.forName(className);

                hints.reflection()
                        .registerType(apiClientClass, MemberCategory.values());

                hints.proxies()
                        .registerJdkProxy(apiClientClass)
                        .registerJdkProxy(apiClientClass, SpringProxy.class, Advised.class, DecoratingProxy.class);

                ApiClient apiClientAnnotation = apiClientClass.getAnnotation(ApiClient.class);
                Class<?> fallbackFactoryClass = apiClientAnnotation.fallbackFactory();
                if (fallbackFactoryClass != Void.class) {
                    registerFallbackFactoryHints(hints, fallbackFactoryClass);
                }

            } catch (ClassNotFoundException e) {
                // ignore
            }
        }

        // WebClient 相关 AOT 提示
        hints.reflection()
                .registerTypeIfPresent(classLoader,
                        "org.springframework.web.reactive.function.client.WebClient",
                        MemberCategory.values())
                .registerTypeIfPresent(classLoader,
                        "org.springframework.web.reactive.function.client.support.WebClientAdapter",
                        MemberCategory.values());

        hints.proxies()
                .registerJdkProxy(
                        org.springframework.aot.hint.TypeReference.of("org.springframework.web.reactive.function.client.WebClient"));
    }

    /**
     * 注册降级工厂的 AOT 提示
     *
     * @param hints             运行时提示
     * @param fallbackFactoryClass 降级工厂类
     */
    private void registerFallbackFactoryHints(RuntimeHints hints, Class<?> fallbackFactoryClass) {
        hints.reflection()
                .registerType(fallbackFactoryClass, MemberCategory.values());

        for (java.lang.reflect.Constructor<?> constructor : fallbackFactoryClass.getDeclaredConstructors()) {
            hints.reflection().registerConstructor(constructor, ExecutableMode.INVOKE);
        }

        try {
            java.lang.reflect.Method createMethod = fallbackFactoryClass.getMethod("create", Throwable.class);
            hints.reflection().registerMethod(createMethod, ExecutableMode.INVOKE);
        } catch (NoSuchMethodException e) {
            // ignore
        }
    }
}
