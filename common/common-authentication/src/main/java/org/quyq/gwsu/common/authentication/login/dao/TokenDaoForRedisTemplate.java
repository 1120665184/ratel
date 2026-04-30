package org.quyq.gwsu.common.authentication.login.dao;


import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.auto.SaTokenDaoByObjectFollowString;
import cn.dev33.satoken.util.SaFoxUtil;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
@RequiredArgsConstructor
public class TokenDaoForRedisTemplate implements SaTokenDaoByObjectFollowString, SaTokenDao {

    private final CacheUtils cacheUtils;

    @Override
    public String get(String key) {
        return cacheUtils.withRebel(()->cacheUtils.get(key));
    }

    @Override
    public void set(String key, String value, long timeout) {
        cacheUtils.withRebel(() ->{

            if (timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE) {
                return 0;
            }

            // 判断是否为永不过期
            if (timeout == SaTokenDao.NEVER_EXPIRE) {
                cacheUtils.set(key, value);
            } else {
                cacheUtils.set(key, value, timeout, TimeUnit.SECONDS);
            }

            return 0;
        });

    }

    @Override
    public void update(String key, String value) {
        long expire = getTimeout(key);
        // -2 = 无此键
        if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        this.set(key, value, expire);
    }

    @Override
    public void delete(String key) {
        cacheUtils.withRebel(() ->cacheUtils.delete(key));
    }

    @Override
    public long getTimeout(String key) {
        return cacheUtils.withRebel(() ->cacheUtils.getExpire(key));
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        // 判断是否想要设置为永久
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getTimeout(key);
            if (expire == SaTokenDao.NEVER_EXPIRE) {
                // 如果其已经被设置为永久，则不作任何处理
            } else {
                // 如果尚未被设置为永久，那么再次set一次
                this.set(key, this.get(key), timeout);
            }
            return;
        }
        cacheUtils.withRebel(() -> cacheUtils.expire(key, timeout, TimeUnit.SECONDS));
    }

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {

        Collection<String> keys = cacheUtils.withRebel(() ->cacheUtils.scan(prefix + "*" + keyword + "*"));
        List<String> list = new ArrayList<>(keys);
        return SaFoxUtil.searchList(list, start, size, sortType);
    }
}
