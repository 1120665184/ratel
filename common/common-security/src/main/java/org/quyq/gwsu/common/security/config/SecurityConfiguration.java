package org.quyq.gwsu.common.security.config;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.captcha.service.CaptchaProvider;
import org.quyq.gwsu.common.security.captcha.service.CaptchaServiceFacade;
import org.quyq.gwsu.common.security.captcha.service.impl.DefaultCaptchaServiceFacade;
import org.quyq.gwsu.common.security.config.properties.SecurityProperties;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/4
 * @description
 */
@AutoConfiguration
@Slf4j
@ImportRuntimeHints(SecurityRuntimeHintsRegistrar.class)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {

    @Bean
    public SecurityUtils securityUtils(CacheUtils cacheUtils) {
        return new SecurityUtils(cacheUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public CaptchaServiceFacade captchaServiceFacade(List<CaptchaProvider> providers) {
        return new DefaultCaptchaServiceFacade(providers);
    }

    @Bean
    public SessionUtils sessionUtils(CacheUtils cacheUtils, SecurityUtils securityUtils, SimpleModule javaTimeModule) {

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)        // 允许 Object.class 的子类型被反序列化
                .allowIfBaseType(Object.class)        // 允许 Object.class 作为基类型（在启用多态时）
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .polymorphicTypeValidator(ptv)
                .activateDefaultTypingAsProperty(ptv, DefaultTyping.NON_FINAL, "@class")
                .addModule(javaTimeModule)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)        // 序列化时如果 bean 没有属性，则不抛出异常
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)        // 反序列化时如果有未知属性，也不抛出异常（兼容性更好）
                .build();

        return new SessionUtils(cacheUtils, securityUtils, mapper);
    }


}
