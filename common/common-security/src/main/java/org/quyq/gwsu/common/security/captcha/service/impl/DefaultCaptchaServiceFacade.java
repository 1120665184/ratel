package org.quyq.gwsu.common.security.captcha.service.impl;

import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaCheckRequest;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaCheckResponse;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaGetRequest;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaGetResponse;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaVerifyRequest;
import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;
import org.quyq.gwsu.common.security.captcha.properties.CaptchaProperties;
import org.quyq.gwsu.common.security.captcha.service.CaptchaProvider;
import org.quyq.gwsu.common.security.captcha.service.CaptchaServiceFacade;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认验证码门面。
 *
 * @author Quyq
 */
public class DefaultCaptchaServiceFacade implements CaptchaServiceFacade {

    private final Map<CaptchaType, CaptchaProvider> providers = new HashMap<>();

    public DefaultCaptchaServiceFacade(List<CaptchaProvider> providers) {
        providers.forEach(provider -> provider.supportTypes()
                .forEach(type -> this.providers.put(type, provider)));
    }

    @Override
    public CaptchaGetResponse get(CaptchaGetRequest request) {
        CaptchaProperties properties = properties();
        AssertUtils.isTrue(properties.isEnabled(), CommonErrorCode.E04013);
        CaptchaType type = properties.effectiveType(request.type());
        return provider(type).get(new CaptchaGetRequest(type, request.scene(), request.clientUid()));
    }

    @Override
    public CaptchaCheckResponse check(CaptchaCheckRequest request) {
        AssertUtils.isTrue(properties().isEnabled(), CommonErrorCode.E04013);
        return providerByCaptchaId(request.captchaId()).check(request);
    }

    @Override
    public void verifyForLogin(CaptchaVerifyRequest request) {
        AssertUtils.hasText(request.captchaId(), CommonErrorCode.E04008);
        AssertUtils.hasText(request.captchaCode(), CommonErrorCode.E04009);
        AssertUtils.isTrue(properties().isEnabled(), CommonErrorCode.E04013);
        providerByCaptchaId(request.captchaId()).verify(request);
    }

    private CaptchaProvider provider(CaptchaType type) {
        CaptchaProvider provider = providers.get(type);
        AssertUtils.notNull(provider, CommonErrorCode.E04012);
        return provider;
    }

    private CaptchaProvider providerByCaptchaId(String captchaId) {
        AssertUtils.hasText(captchaId, CommonErrorCode.E04008);
        return provider(parseTypeFromCaptchaId(captchaId));
    }

    private CaptchaType parseTypeFromCaptchaId(String captchaId) {
        int separator = captchaId.indexOf(":");
        if (separator <= 0) {
            throw new SecurityException(CommonErrorCode.E04010);
        }
        return CaptchaType.from(captchaId.substring(0, separator));
    }

    private CaptchaProperties properties() {
        return ConfigInfoUtils.getByObject(CaptchaProperties.CONFIG_KEY, CaptchaProperties.class);
    }
}
