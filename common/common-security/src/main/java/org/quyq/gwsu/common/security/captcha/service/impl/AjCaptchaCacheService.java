package org.quyq.gwsu.common.security.captcha.service.impl;

import com.anji.captcha.service.CaptchaCacheService;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;

import java.time.Duration;

/**
 * AJ-Captcha Redis 缓存适配。
 *
 * @author Quyq
 */
public class AjCaptchaCacheService implements CaptchaCacheService {

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        cacheUtils().set(key, value, Duration.ofSeconds(expiresInSeconds));
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
}
