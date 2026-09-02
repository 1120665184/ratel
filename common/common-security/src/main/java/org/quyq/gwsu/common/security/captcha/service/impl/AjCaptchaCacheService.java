package org.quyq.gwsu.common.security.captcha.service.impl;

import com.anji.captcha.service.CaptchaCacheService;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.captcha.properties.CaptchaProperties;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;

import java.time.Duration;

/**
 * AJ-Captcha Redis 缓存适配。
 *
 * @author Quyq
 */
public class AjCaptchaCacheService implements CaptchaCacheService {

    private static final String CAPTCHA_KEY_PREFIX = "RUNNING:CAPTCHA:";
    private static final String SECOND_CAPTCHA_KEY_PREFIX = "RUNNING:CAPTCHA:second-";

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        cacheUtils().set(key, value, Duration.ofSeconds(effectiveExpiresInSeconds(key, expiresInSeconds)));
    }

    @Override
    public boolean exists(String key) {
        return cacheUtils().get(key) != null;
    }

    @Override
    public void delete(String key) {
        cacheUtils().delete(key);
    }

    @Override
    public String get(String key) {
        return cacheUtils().get(key);
    }

    @Override
    public String type() {
        return "redis";
    }

    public Long increment(String key, long val) {
        return cacheUtils().increment(key, val);
    }

    private CacheUtils cacheUtils() {
        return SpringUtils.getBean(CacheUtils.class);
    }

    private long effectiveExpiresInSeconds(String key, long expiresInSeconds) {
        if (key == null || !key.startsWith(CAPTCHA_KEY_PREFIX)) {
            return expiresInSeconds;
        }
        CaptchaProperties properties = ConfigInfoUtils.getByObject(CaptchaProperties.CONFIG_KEY, CaptchaProperties.class);
        if (key.startsWith(SECOND_CAPTCHA_KEY_PREFIX)) {
            return properties.effectiveVerificationExpireSeconds();
        }
        return properties.effectiveExpireSeconds();
    }
}
