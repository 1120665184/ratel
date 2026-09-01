package org.quyq.gwsu.common.security.captcha.service;

import org.quyq.gwsu.common.security.captcha.domain.CaptchaCheckRequest;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaCheckResponse;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaGetRequest;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaGetResponse;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaVerifyRequest;
import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;

import java.util.Set;

/**
 * 验证码提供者。
 *
 * @author Quyq
 */
public interface CaptchaProvider {

    Set<CaptchaType> supportTypes();

    CaptchaGetResponse get(CaptchaGetRequest request);

    CaptchaCheckResponse check(CaptchaCheckRequest request);

    void verify(CaptchaVerifyRequest request);
}
