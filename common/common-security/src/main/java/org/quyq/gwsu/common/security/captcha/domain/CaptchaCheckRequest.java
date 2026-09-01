package org.quyq.gwsu.common.security.captcha.domain;

/**
 * 一次校验验证码请求。
 *
 * @author Quyq
 */
public record CaptchaCheckRequest(
        String captchaId,
        String captchaCode,
        String pointJson
) {
}
