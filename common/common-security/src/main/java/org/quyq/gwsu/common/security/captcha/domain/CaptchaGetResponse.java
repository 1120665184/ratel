package org.quyq.gwsu.common.security.captcha.domain;

import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;

import java.util.Map;

/**
 * 获取验证码响应。
 *
 * @author Quyq
 */
public record CaptchaGetResponse(
        CaptchaType type,
        String captchaId,
        long expireSeconds,
        long verificationExpireSeconds,
        Map<String, Object> data
) {
}
