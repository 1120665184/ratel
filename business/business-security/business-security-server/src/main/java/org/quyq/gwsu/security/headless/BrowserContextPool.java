package org.quyq.gwsu.security.headless;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Browser.NewContextOptions;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 预创建 BrowserContext 池
 * <p>
 * 基于 BlockingQueue 实现，初始化时预创建 minIdle 个 BrowserContext，
 * 每次使用时 borrow() 获取，用完后销毁并补充新实例到池中。
 * 超过 maxTotal 的请求阻塞等待。
 * <p>
 * 注意：Playwright 的 BrowserContext 无法完全重置（cookie/storage 残留），
 * 因此每次用完后直接销毁，再新建一个干净的 BrowserContext 补充到池中。
 */
@Slf4j
public class BrowserContextPool implements AutoCloseable {

    private final Browser browser;
    private final int minIdle;
    private final int maxTotal;
    private final BlockingQueue<BrowserContext> idleQueue;
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private volatile boolean closed = false;

    public BrowserContextPool(Browser browser, int minIdle, int maxTotal) {
        this.browser = browser;
        this.minIdle = minIdle;
        this.maxTotal = maxTotal;
        this.idleQueue = new LinkedBlockingQueue<>(maxTotal);

        // 预创建 minIdle 个 BrowserContext
        for (int i = 0; i < minIdle; i++) {
            try {
                BrowserContext ctx = createNewContext();
                idleQueue.offer(ctx);
            } catch (Exception e) {
                log.warn("预创建 BrowserContext 失败 (第{}个): {}", i + 1, e.getMessage());
            }
        }

        log.info("BrowserContextPool 初始化完成: minIdle={}, maxTotal={}, 预创建={}",
                minIdle, maxTotal, idleQueue.size());
    }

    /**
     * 从池中获取一个 BrowserContext
     * 优先从空闲队列获取，如果池为空且未超过上限则创建新的
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return BrowserContext 实例
     * @throws RuntimeException 获取超时或池已关闭
     */
    public BrowserContext borrow(long timeout, TimeUnit unit) {
        if (closed) throw new IllegalStateException("BrowserContextPool 已关闭");

        // 1. 先尝试从空闲队列获取
        BrowserContext ctx = idleQueue.poll();
        if (ctx != null) {
            try {
                // 验证 context 仍然有效
                ctx.cookies();
                return ctx;
            } catch (Exception e) {
                log.debug("空闲 BrowserContext 已失效，丢弃并创建新的");
                totalCreated.decrementAndGet();
                safeClose(ctx);
            }
        }

        // 2. 尝试创建新的
        int currentTotal = totalCreated.get();
        if (currentTotal < maxTotal) {
            if (totalCreated.compareAndSet(currentTotal, currentTotal + 1)) {
                try {
                    return createNewContext();
                } catch (Exception e) {
                    totalCreated.decrementAndGet();
                    throw new RuntimeException("创建 BrowserContext 失败", e);
                }
            }
        }

        // 3. 等待其他请求归还
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
     * 销毁已使用的 BrowserContext 并补充新实例到池中
     * <p>
     * 由于 Playwright BrowserContext 无法完全重置，每次用完直接销毁，
     * 然后创建一个新的干净实例补充到池中。
     *
     * @param ctx 已使用完毕的 BrowserContext
     */
    public void returnAndReplenish(BrowserContext ctx) {
        if (ctx == null) return;

        // 销毁旧 context
        safeClose(ctx);
        totalCreated.decrementAndGet();

        // 补充新实例到池中
        if (!closed) {
            try {
                BrowserContext newCtx = createNewContext();
                totalCreated.incrementAndGet();
                if (!idleQueue.offer(newCtx)) {
                    // 队列满，销毁多余的
                    safeClose(newCtx);
                    totalCreated.decrementAndGet();
                }
            } catch (Exception e) {
                log.warn("补充 BrowserContext 失败: {}", e.getMessage());
            }
        }
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
        BrowserContext ctx;
        while ((ctx = idleQueue.poll()) != null) {
            safeClose(ctx);
        }
        totalCreated.set(0);
        log.info("BrowserContextPool 已关闭");
    }

    private BrowserContext createNewContext() {
        return browser.newContext(new NewContextOptions()
                .setViewportSize(1920, 1080)
                .setDeviceScaleFactor(1.0));
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
