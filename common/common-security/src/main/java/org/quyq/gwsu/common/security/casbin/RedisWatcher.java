package org.quyq.gwsu.common.security.casbin;


import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.persist.Watcher;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Quyq
 * @date 2026/4/4
 * @description
 */
@Slf4j
public class RedisWatcher implements Watcher, DisposableBean {

    private final CacheUtils cacheUtils;

    private RedisMessageListenerContainer listenerContainer = null;


    public RedisWatcher(CacheUtils cacheUtils) {
        this.cacheUtils = cacheUtils;
    }

    @Override
    public void setUpdateCallback(Runnable runnable) {
        listenerContainer = cacheUtils.withRebel(() ->
                cacheUtils.addListener(SecurityConstants.Abac.PERMISSION_CHANGE_NOTICE_TOPIC,
                        (message, pattern) -> {
                            Object msg = cacheUtils.getSerializer().deserialize(message.getBody());
                            if(msg instanceof String val) {
                                if ("syncAccess".equals(val) && runnable != null) {
                                    runnable.run();
                                }
                            }

                        })
        );

    }

    @Override
    public void setUpdateCallback(Consumer<String> consumer) {
        //ignore
    }

    @Override
    public void update() {
        //ignore
    }

    @Override
    public void destroy() throws Exception {
        if (Objects.nonNull(listenerContainer)) {
            listenerContainer.stop();
        }
    }
}
