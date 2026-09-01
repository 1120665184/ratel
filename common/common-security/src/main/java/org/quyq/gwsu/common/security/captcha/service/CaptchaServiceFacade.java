package org.quyq.gwsu.common.security.captcha.service;

import org.quyq.gwsu.common.security.captcha.domain.CaptchaCheckRequest;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaCheckResponse;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaGetRequest;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaGetResponse;
import org.quyq.gwsu.common.security.captcha.domain.CaptchaVerifyRequest;

/**
 * 验证码统一门面。
 *
 * @author Quyq
 */
public interface CaptchaServiceFacade {

    CaptchaGetResponse get(CaptchaGetRequest request);

    CaptchaCheckResponse check(CaptchaCheckRequest request);

    void verifyForLogin(CaptchaVerifyRequest request);
}
