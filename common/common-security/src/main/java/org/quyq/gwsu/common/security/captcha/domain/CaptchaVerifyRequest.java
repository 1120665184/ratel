package org.quyq.gwsu.common.security.captcha.domain;

/**
 * 登录前验证码校验请求。
 *
 * @author Quyq
 */
public record CaptchaVerifyRequest(
        String captchaId,
        String captchaCode,
        String loginType,
        String scene
) {
}
