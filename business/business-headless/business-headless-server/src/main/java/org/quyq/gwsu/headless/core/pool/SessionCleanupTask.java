package org.quyq.gwsu.headless.core.pool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.headless.config.HeadlessBrowserConfiguration;

/**
 * 缓存 Session 定时清理任务
 * <p>
 * 定期扫描 sessionCache，清理沉寂超过 sessionMaxIdleMinutes 的 Session，
 * 防止长期不活跃的 Session 占用浏览器资源。
 * <p>
 * 调度由 HeadlessBrowserConfiguration 中配置的 SchedulingConfigurer 驱动。
 */
@Slf4j
@RequiredArgsConstructor
public class SessionCleanupTask {

    private final BrowserContextPool contextPool;
    private final HeadlessBrowserConfiguration config;

    /**
     * 执行一次清理
     */
    public void cleanup() {
        if (!config.isSessionCacheEnabled()) return;

        try {
            int evicted = contextPool.evictIdleSessions(config.getSessionMaxIdleMinutes());
            if (evicted > 0) {
                log.info("定时清理: 淘汰 {} 个沉寂超时 Session (超时={}分钟, 缓存剩余={})",
                        evicted, config.getSessionMaxIdleMinutes(), contextPool.cachedSessionCount());
            } else {
                log.debug("定时清理: 无需清理 (缓存数={})", contextPool.cachedSessionCount());
            }
        } catch (Exception e) {
            log.error("定时清理 Session 异常", e);
        }
    }
}
