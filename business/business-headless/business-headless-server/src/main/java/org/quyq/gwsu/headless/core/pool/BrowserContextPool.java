package org.quyq.gwsu.headless.core.pool;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Browser.NewContextOptions;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.headless.config.HeadlessBrowserConfiguration;
import org.quyq.gwsu.headless.core.session.HeadlessBrowserSession;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 浏览器上下文双层池
 * <p>
 * 第一层：idleQueue — 空白 BrowserContext 池，冷启动时使用
 * 第二层：sessionCache — 按 userId 缓存的已认证 Session，热复用时使用
 * <p>
 * 容量控制：idleQueue 中的实例 + sessionCache 中的实例 ≤ maxTotal
 */
@Slf4j
public class BrowserContextPool implements AutoCloseable {

    private final Browser browser;
    private final HeadlessBrowserConfiguration config;

    // ==================== 第一层：空白 BrowserContext 池 ====================
    private final BlockingQueue<BrowserContext> idleQueue;
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private volatile boolean closed = false;

    // ==================== 第二层：按 userId 缓存的 Session 池 ====================
    private final Map<String, SessionWrapper> sessionCache = new ConcurrentHashMap<>();

    public BrowserContextPool(Browser browser, HeadlessBrowserConfiguration config) {
        this.browser = browser;
        this.config = config;
        this.idleQueue = new LinkedBlockingQueue<>(config.getMaxContexts());

        // 预创建 minIdle 个 BrowserContext
        for (int i = 0; i < config.getMinIdle(); i++) {
            try {
                BrowserContext ctx = createNewContext();
                totalCreated.incrementAndGet();
                idleQueue.offer(ctx);
            } catch (Exception e) {
                log.warn("预创建 BrowserContext 失败 (第{}个): {}", i + 1, e.getMessage());
            }
        }

        log.info("BrowserContextPool 初始化完成: minIdle={}, maxTotal={}, sessionCacheEnabled={}, sessionEvictIdleMinutes={}, sessionMaxIdleMinutes={}",
                config.getMinIdle(), config.getMaxContexts(),
                config.isSessionCacheEnabled(), config.getSessionEvictIdleMinutes(), config.getSessionMaxIdleMinutes());
    }

    // ==================== Session 缓存操作 ====================

    /**
     * 从缓存中获取用户的 Session
     *
     * @param userId 用户 ID
     * @return SessionWrapper，缓存未命中返回 null
     */
    public SessionWrapper getCachedSession(String userId) {
        if (!config.isSessionCacheEnabled()) return null;

        SessionWrapper wrapper = sessionCache.get(userId);
        if (wrapper == null) return null;

        // CAS：IDLE → ACTIVE，无锁状态转换
        if (wrapper.compareAndSetState(SessionState.IDLE, SessionState.ACTIVE)) {
            log.debug("缓存命中: userId={}, 沉寂时长={}s", userId, wrapper.idleDurationSeconds());
            return wrapper;
        }
        // ACTIVE 或 EVICTING 状态，不可复用
        return null;
    }

    /**
     * 将 Session 放入缓存（操作完成后调用）
     *
     * @param userId  用户 ID
     * @param session 已认证的 Session
     * @return true=缓存成功，false=缓存未启用
     */
    public boolean cacheSession(String userId, HeadlessBrowserSession session) {
        if (!config.isSessionCacheEnabled()) return false;

        SessionWrapper wrapper = new SessionWrapper(session, userId);

        // 如果已有旧缓存，先移除
        SessionWrapper old = sessionCache.put(userId, wrapper);
        if (old != null) {
            log.debug("替换旧缓存: userId={}", userId);
            destroyWrapper(old);
        }

        // 检查缓存数量是否超限，超限则淘汰最久沉寂的
        evictIfOverLimit();

        log.debug("Session 已缓存: userId={}, 当前缓存数={}", userId, sessionCache.size());
        return true;
    }

    /**
     * 将 Session 标记为 IDLE（操作完成后调用）
     *
     * @param userId 用户 ID
     */
    public void markIdle(String userId) {
        SessionWrapper wrapper = sessionCache.get(userId);
        if (wrapper != null) {
            // CAS：ACTIVE → IDLE
            wrapper.compareAndSetState(SessionState.ACTIVE, SessionState.IDLE);
        }
    }

    /**
     * 从缓存中移除并销毁指定用户的 Session
     */
    public void removeCachedSession(String userId) {
        SessionWrapper wrapper = sessionCache.remove(userId);
        if (wrapper != null) {
            destroyWrapper(wrapper);
            log.debug("已移除缓存 Session: userId={}", userId);
        }
    }

    /**
     * 清理沉寂超过指定时间的缓存 Session（供定时任务和借用时淘汰调用）
     *
     * @param idleMinutes 沉寂超时分钟数
     * @return 清理的数量
     */
    public int evictIdleSessions(int idleMinutes) {
        List<String> toEvict = new ArrayList<>();

        for (Map.Entry<String, SessionWrapper> entry : sessionCache.entrySet()) {
            SessionWrapper wrapper = entry.getValue();
            // CAS：IDLE → EVICTING
            if (wrapper.getState() == SessionState.IDLE
                    && wrapper.isIdleLongerThan(idleMinutes)
                    && wrapper.compareAndSetState(SessionState.IDLE, SessionState.EVICTING)) {
                toEvict.add(entry.getKey());
            }
        }

        for (String userId : toEvict) {
            SessionWrapper wrapper = sessionCache.remove(userId);
            if (wrapper != null) {
                destroyWrapper(wrapper);
                log.info("淘汰沉寂超时 Session: userId={}, 沉寂时长={}s", userId, wrapper.idleDurationSeconds());
            }
        }

        return toEvict.size();
    }

    /**
     * 获取缓存 Session 数量
     */
    public int cachedSessionCount() {
        return sessionCache.size();
    }

    // ==================== 空白 BrowserContext 池操作 ====================

    /**
     * 从空白池中获取一个 BrowserContext（首次访问用户使用）
     */
    public BrowserContext borrowIdle(long timeout, TimeUnit unit) {
        if (closed) throw new IllegalStateException("BrowserContextPool 已关闭");

        // 1. 先尝试从空闲队列获取
        BrowserContext ctx = idleQueue.poll();
        if (ctx != null) {
            try {
                ctx.cookies();
                return ctx;
            } catch (Exception e) {
                log.debug("空闲 BrowserContext 已失效，丢弃并创建新的");
                totalCreated.decrementAndGet();
                safeClose(ctx);
            }
        }

        // 2. 尝试淘汰沉寂超时的缓存 Session，释放名额
        if (config.isSessionCacheEnabled() && totalCreated.get() >= config.getMaxContexts()) {
            int evicted = evictIdleSessions(config.getSessionEvictIdleMinutes());
            if (evicted > 0) {
                log.debug("淘汰了 {} 个沉寂 Session，释放名额", evicted);
            }
        }

        // 3. 尝试创建新的
        int currentTotal = totalCreated.get();
        if (currentTotal < config.getMaxContexts()) {
            if (totalCreated.compareAndSet(currentTotal, currentTotal + 1)) {
                try {
                    return createNewContext();
                } catch (Exception e) {
                    totalCreated.decrementAndGet();
                    throw new RuntimeException("创建 BrowserContext 失败", e);
                }
            }
        }

        // 4. 等待其他请求归还
        try {
            ctx = idleQueue.poll(timeout, unit);
            if (ctx != null) {
                return ctx;
            }
            throw new RuntimeException("获取 BrowserContext 超时");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取 BrowserContext 被中断", e);
        }
    }

    /**
     * 归还空白 BrowserContext 到池中（非缓存场景使用）
     */
    public void returnIdle(BrowserContext ctx) {
        if (ctx == null) return;
        if (!closed && idleQueue.size() < config.getMinIdle()) {
            // 还需要补充空闲池
            try {
                ctx.cookies();
                if (idleQueue.offer(ctx)) return;
            } catch (Exception e) {
                log.debug("归还的 BrowserContext 已失效");
            }
        }
        // 不需要了，销毁
        safeClose(ctx);
        totalCreated.decrementAndGet();
    }

    /**
     * 销毁已使用的 BrowserContext 并补充新实例到池中
     */
    public void returnAndReplenish(BrowserContext ctx) {
        if (ctx == null) return;

        safeClose(ctx);
        totalCreated.decrementAndGet();

        if (!closed) {
            try {
                BrowserContext newCtx = createNewContext();
                totalCreated.incrementAndGet();
                if (!idleQueue.offer(newCtx)) {
                    safeClose(newCtx);
                    totalCreated.decrementAndGet();
                }
            } catch (Exception e) {
                log.warn("补充 BrowserContext 失败: {}", e.getMessage());
            }
        }
    }

    // ==================== 资源管理 ====================

    /**
     * 增加总实例计数（缓存 Session 从 idleQueue 借用时调用）
     */
    public void incrementTotal() {
        totalCreated.incrementAndGet();
    }

    /**
     * 获取当前池中空闲数量
     */
    public int idleCount() {
        return idleQueue.size();
    }

    /**
     * 获取已创建的总数量
     */
    public int totalCount() {
        return totalCreated.get();
    }

    @Override
    public void close() {
        closed = true;

        // 1. 销毁所有缓存 Session
        for (Map.Entry<String, SessionWrapper> entry : sessionCache.entrySet()) {
            destroyWrapper(entry.getValue());
        }
        sessionCache.clear();

        // 2. 销毁空闲池
        BrowserContext ctx;
        while ((ctx = idleQueue.poll()) != null) {
            safeClose(ctx);
        }
        totalCreated.set(0);
        log.info("BrowserContextPool 已关闭");
    }

    // ==================== 内部方法 ====================

    /**
     * 缓存数量超限时淘汰最久沉寂的 Session
     */
    private void evictIfOverLimit() {
        while (sessionCache.size() > config.getMaxCachedSessions()) {
            Optional<Map.Entry<String, SessionWrapper>> oldest = sessionCache.entrySet().stream()
                    .filter(e -> e.getValue().getState() == SessionState.IDLE)
                    .min(Comparator.comparingLong(e -> e.getValue().getLastActiveTime().getEpochSecond()));

            if (oldest.isEmpty()) break;

            String userId = oldest.get().getKey();
            SessionWrapper wrapper = sessionCache.remove(userId);
            if (wrapper != null) {
                destroyWrapper(wrapper);
                log.info("缓存超限淘汰: userId={}, 当前缓存数={}", userId, sessionCache.size());
            }
        }
    }

    private BrowserContext createNewContext() {
        return browser.newContext(new NewContextOptions()
                .setViewportSize(1920, 1080)
                .setDeviceScaleFactor(1.0));
    }

    /**
     * 销毁 SessionWrapper（关闭 Session 中的浏览器资源，减少总计数）
     */
    private void destroyWrapper(SessionWrapper wrapper) {
        try {
            wrapper.getSession().close();
        } catch (Exception e) {
            log.warn("销毁缓存 Session 异常: userId={}", wrapper.getUserId(), e);
        }
        totalCreated.decrementAndGet();
    }

    private void safeClose(BrowserContext ctx) {
        if (ctx == null) return;
        try {
            ctx.close();
        } catch (Exception e) {
            log.debug("关闭 BrowserContext 异常: {}", e.getMessage());
        }
    }
}
