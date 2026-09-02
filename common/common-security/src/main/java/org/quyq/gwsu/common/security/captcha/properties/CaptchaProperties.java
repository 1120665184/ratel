package org.quyq.gwsu.common.security.captcha.properties;

import lombok.Data;
import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;

/**
 * 验证码配置，对应 security_config.captcha_config。
 *
 * @author Quyq
 */
@Data
public class CaptchaProperties {

    public static final String CONFIG_KEY = "captcha_config";
    public static final String DEFAULT_WATER_MARK = "Ratel-Manager";
    public static final long DEFAULT_EXPIRE_SECONDS = 120L;
    public static final long DEFAULT_VERIFICATION_EXPIRE_SECONDS = 180L;

    /**
     * 默认验证码类型。
     */
    private CaptchaType type = CaptchaType.BLOCK_PUZZLE;

    /**
     * 是否启用验证码能力。
     */
    private boolean enabled = true;

    /**
     * 验证码水印文字。
     */
    private String waterMark = DEFAULT_WATER_MARK;

    /**
     * 验证码挑战有效时间，单位秒。
     */
    private Long expireSeconds = DEFAULT_EXPIRE_SECONDS;

    /**
     * 二次校验凭证有效时间，单位秒。
     */
    private Long verificationExpireSeconds = DEFAULT_VERIFICATION_EXPIRE_SECONDS;

    public CaptchaType effectiveType(CaptchaType requestType) {
        if (requestType != null) {
            return requestType;
        }
        return type == null ? CaptchaType.BLOCK_PUZZLE : type;
    }

    public long effectiveExpireSeconds() {
        return effectiveSeconds(expireSeconds, DEFAULT_EXPIRE_SECONDS);
    }

    public long effectiveVerificationExpireSeconds() {
        return effectiveSeconds(verificationExpireSeconds, DEFAULT_VERIFICATION_EXPIRE_SECONDS);
    }

    public String effectiveWaterMark() {
        return waterMark == null || waterMark.isBlank() ? DEFAULT_WATER_MARK : waterMark;
    }

    private long effectiveSeconds(Long seconds, long defaultSeconds) {
        return seconds == null || seconds <= 0 ? defaultSeconds : seconds;
    }

}
