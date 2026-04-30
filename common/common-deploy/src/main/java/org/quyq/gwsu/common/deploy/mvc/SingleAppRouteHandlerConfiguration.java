package org.quyq.gwsu.common.deploy.mvc;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Quyq
 * @date 2026/3/10
 * @description 单机部署时，接口统一添加模块前缀，使接口路径和分布式部署一致
 */
@RequiredArgsConstructor
public class SingleAppRouteHandlerConfiguration implements WebMvcConfigurer {


    private final List<BusinessModuleInfoProvider> moduleInfoProviders;


    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        if (CollectionUtils.isEmpty(moduleInfoProviders)) {
            return;
        }

        for (BusinessModuleInfoProvider provider : moduleInfoProviders) {

            String basePackage = Stream.of(provider.getClass().getPackageName().split("\\."))
                    .limit(4).collect(Collectors.joining("."));
            configurer.addPathPrefix("/%s".formatted(provider.module().prefix()), clazz -> clazz.getPackageName().startsWith(basePackage));
        }

    }
}
