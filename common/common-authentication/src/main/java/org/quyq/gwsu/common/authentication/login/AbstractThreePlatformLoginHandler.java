package org.quyq.gwsu.common.authentication.login;


import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.common.authentication.config.properties.LoginProperties;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.login.domain.ThreePlatformConfig;
import org.quyq.gwsu.common.authentication.login.domain.ThreePlatformLoginDTO;
import org.quyq.gwsu.common.authentication.login.domain.WebCallInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.config.properties.universal.BaseProjectInfoProperties;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description 对接第三方平台、门户认证实现抽象类
 * <p>
 *
 */
public abstract class AbstractThreePlatformLoginHandler<T extends UserInfo> extends AbstractLoginHandler<ThreePlatformLoginDTO, T> {


    /**
     * 获取第三方平台的配置信息
     *
     * @return
     */
    protected ThreePlatformConfig getConfig() {
        LoginProperties properties = SpringUtils.getBean(LoginProperties.class);
        Optional<ThreePlatformConfig> config = Optional.ofNullable(properties.getThreePlatform().get(loginType()));
        if (config.isPresent()) {
            ThreePlatformConfig copy = config.get().copy();
            BaseProjectInfoProperties baseProjectInfoProperties = ConfigInfoUtils.getByObject(BaseProjectInfoProperties.CONFIG_KEY, BaseProjectInfoProperties.class);
            if (StringUtils.hasText(copy.getRedirectUrl())) {
                copy.setRedirectUrl(replaceUrl(baseProjectInfoProperties, copy.getRedirectUrl()));
            }
            if (!CollectionUtils.isEmpty(copy.getProperties())) {
                Map<String, String> newV = new HashMap<>(copy.getProperties().size());
                copy.getProperties().forEach((k, v) -> newV.put(k, replaceUrl(baseProjectInfoProperties, v)));
                copy.setProperties(newV);
            }
            return copy;
        }
        throw new AuthException(CommonErrorCode.E04003, "三方登录方式【%s】缺少必要的配置信息，请联系管理员配置".formatted(loginType()));
    }

    private String replaceUrl(BaseProjectInfoProperties baseProjectInfoProperties, String old) {
        return old.replace("<apiBaseUrl>", baseProjectInfoProperties.apiBaseUrl())
                .replace("<viewBaseUrl>", baseProjectInfoProperties.viewBaseUrl());
    }

    /**
     * 生成前端跳转三方的认证界面url
     *
     * @param config
     * @param state
     * @return
     */
    protected abstract @NonNull WebCallInfo authUrl(ThreePlatformConfig config, String state);

    /**
     * 三方接口认证回调逻辑
     *
     * @param loginVO
     * @param config
     * @return
     */
    protected abstract T callback(ThreePlatformLoginDTO loginVO, ThreePlatformConfig config);


    @Override
    protected T toAuth(ThreePlatformLoginDTO loginVO, CoreProperties properties) {
        ThreePlatformConfig config = getConfig();
        T callback = callback(loginVO, config);
        properties.setRedirect(config.isRedirect());
        properties.setRedirectUrl(config.getRedirectUrl());
        return callback;
    }

    @Override
    public WebCallInfo generateWebCallInfo(String state) {
        ThreePlatformConfig config = getConfig();
        return authUrl(config, state);
    }
}
