package org.quyq.gwsu.common.security.config;


import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.main.SyncedEnforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.util.function.CustomFunction;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.security.casbin.RedisAdapter;
import org.quyq.gwsu.common.security.casbin.RedisWatcher;
import org.quyq.gwsu.common.security.casbin.field.FieldEnforcer;
import org.quyq.gwsu.common.security.casbin.function.ContainsFunction;
import org.quyq.gwsu.common.security.casbin.function.IsUserLoginFunction;
import org.quyq.gwsu.common.security.config.properties.SecurityProperties;
import org.quyq.gwsu.common.security.filter.AuthenticationFilter;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/5
 * @description
 */
@AutoConfiguration
@Conditional({CasbinConfiguration.LoadingCondition.class})
@Slf4j
public class CasbinConfiguration {


    @Bean
    public RedisWatcher redisWatcher(CacheUtils cacheUtils) {
        return new RedisWatcher(cacheUtils);
    }


    @Bean
    public Enforcer casbinEnforcer(ResourceLoader resourceLoader, CacheUtils cacheUtils,
                                   RedisWatcher redisWatcher, List<CustomFunction> functions) throws IOException {
        Model model = new Model();
        model.loadModelFromText(loadModelConfig(resourceLoader));

        RedisAdapter adapter = new RedisAdapter(cacheUtils);

        Enforcer enforcer = new SyncedEnforcer(model, adapter);
        //预先加载一次
        enforcer.loadPolicy();
        //配置更新监听
        redisWatcher.setUpdateCallback(() -> {
            enforcer.loadPolicy();
            log.info("权限策略变更， 加载最新策略");
        });
        enforcer.setWatcher(redisWatcher);

        if (CollectionUtils.isNotEmpty(functions)) {
            functions.forEach(function -> enforcer.addFunction(function.getName(), function));
        }

        return enforcer;

    }

    @Bean
    public FieldEnforcer fieldEnforcer(CacheUtils cacheUtils, Enforcer enforcer) {
        return new FieldEnforcer(cacheUtils, enforcer);
    }

    @Bean
    public AuthenticationFilter authenticationFilter(Enforcer enforcer, FieldEnforcer fieldEnforcer,
                                                     SecurityUtils securityUtils,
                                                     SecurityProperties securityProperties) {
        return new AuthenticationFilter(enforcer, fieldEnforcer, securityUtils, securityProperties);
    }


    @Bean
    public ContainsFunction containsFunction() {
        return new ContainsFunction();
    }

    @Bean
    public IsUserLoginFunction isLoginFunction() {
        return new IsUserLoginFunction();
    }


    private String loadModelConfig(ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:casbin/abac.conf");
        try (InputStream is = resource.getInputStream()) {

            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }


    /**
     * 只有网关服务或单应用部署时加载该应用
     */
    static class LoadingCondition extends AnyNestedCondition {
        public LoadingCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(name = CoreConstants.Yaml.DEPLOY_SINGLE, havingValue = "true")
        static class SingleDeployCondition {
        }

        @ConditionalOnClass(name = "org.springframework.cloud.gateway.config.GatewayAutoConfiguration")
        static class GatewayClassCondition {
        }

    }
}
