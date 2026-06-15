package org.quyq.gwsu.common.authentication.config;


import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.plugin.SaTokenPluginHolder;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaStrategy;
import cn.hutool.jwt.JWTUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptor;
import org.quyq.gwsu.common.authentication.login.interceptor.LoginInterceptorUtils;
import org.quyq.gwsu.common.authentication.login.logic.CommonLogic;
import org.quyq.gwsu.common.authentication.login.logic.ProxyLogic;
import org.quyq.gwsu.common.authentication.login.token.SaJsonTemplateForJackson3;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.databind.module.SimpleModule;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InitRunner implements ApplicationRunner {

    @Resource
    private SaTokenDao saTokenDao;

    @Resource
    private SimpleModule javaTimeModule;

    @Resource
    private ObjectProvider<List<LoginInterceptor<?>>> loginInterceptors;


    public InitRunner(SaTokenConfig tokenConfig) {
        //固定该值，禁止修改
        tokenConfig.setTokenName(SecurityConstants.Authentication.AUTH_INFO_KEY_PREFIX);
        tokenConfig.setTokenPrefix(CoreConstants.Headers.TOKEN_PREFIX.trim());
        SaManager.setConfig(tokenConfig);

        StpUtil.stpLogic = new ProxyLogic();
    }

    @Override
    public void run(ApplicationArguments args) {


        //初始化路由匹配策略
        initRouteMatcher();

        //初始化Logic创建函数
        initCreateLogic();

        //初始化token生成
        initCreateTokenFunction();

        //基于redis
        SaManager.setSaTokenDao(saTokenDao);

        //初始化序列化模板
        SaManager.setSaJsonTemplate(new SaJsonTemplateForJackson3(javaTimeModule));

        //初始化登录拦截器工具类
        LoginInterceptorUtils.setInterceptors(loginInterceptors.getIfAvailable(Collections::emptyList));


        //加载插件
        SaTokenPluginHolder.instance.init();

    }


    private void initRouteMatcher() {
        SaStrategy.instance.routeMatcher = (pattern, path) -> {
            PathPattern pathPattern = PathPatternParser.defaultInstance.parse(pattern);
            PathContainer pathContainer = PathContainer.parsePath(path);
            return pathPattern.matches(pathContainer);
        };
    }

    private void initCreateLogic() {
        SaStrategy.instance.createStpLogic = CommonLogic::new;
    }


    /**
     * 初始化token生成
     */
    private void initCreateTokenFunction() {
        SaStrategy.instance.createToken = (loginId, loginType) ->
                JWTUtil.createToken(Map.of(SecurityConstants.JWT.LOGIN_ID_KEY, loginId
                                , SecurityConstants.JWT.LOGIN_TYPE_KEY, loginType,
                                SecurityConstants.JWT.LOGIN_CREATE_MILLIS_KEY, String.valueOf(System.currentTimeMillis())),
                        SecurityConstants.JWT.AUTH_JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8))
        ;

    }

}
