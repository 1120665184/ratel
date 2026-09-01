package org.quyq.gwsu.common.security.captcha.domain;

import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;

/**
 * 获取验证码请求。
 *
 * @author Quyq
 */
public record CaptchaGetRequest(
        CaptchaType type,
        String scene,
        String clientUid
) {
}
