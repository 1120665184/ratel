package org.quyq.gwsu.common.security.captcha.domain;

import java.util.Map;

/**
 * 一次校验验证码响应。
 *
 * @author Quyq
 */
public record CaptchaCheckResponse(
        String captchaId,
        String captchaCode,
        Map<String, Object> extraData
) {
}
