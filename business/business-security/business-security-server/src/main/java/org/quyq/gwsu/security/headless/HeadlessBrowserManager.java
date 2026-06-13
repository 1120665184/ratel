package org.quyq.gwsu.security.headless;

import com.microsoft.playwright.*;
import io.agentscope.core.agui.event.AguiEvent;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.security.headless.config.HeadlessBrowserConfiguration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 无头浏览器管理器
 *
 * 职责：
 * 1. 管理 Playwright 和 Browser 实例（全局共享一个 Browser 进程）
 * 2. 维护用户 Session 池（ConcurrentHashMap<userId, Session>）
 * 3. 并发控制（Semaphore 限制最大 BrowserContext 数量）
 * 4. 闲置超时回收（定时扫描，超过 idleTimeoutMs 未访问的 Session 自动关闭）
 * 5. 提供统一的 sendMessage 接口
 */
@Slf4j
public class HeadlessBrowserManager implements AutoCloseable {

    private final HeadlessBrowserConfiguration config;
    private final Playwright playwright;
    private final Browser browser;
    private final ConcurrentHashMap<String, HeadlessBrowserSession> sessions = new ConcurrentHashMap<>();
    private final Semaphore concurrencyLimiter;
    private final ScheduledExecutorService idleChecker;

    public HeadlessBrowserManager(HeadlessBrowserConfiguration config) {
        this.config = config;
        this.concurrencyLimiter = new Semaphore(config.getMaxContexts());

        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(config.isHeadless()));

        this.idleChecker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "headless-idle-checker");
            t.setDaemon(true);
            return t;
        });
        this.idleChecker.scheduleAtFixedRate(
                this::checkIdleSessions, 1, 1, TimeUnit.MINUTES);

        log.info("HeadlessBrowserManager 初始化完成: maxContexts={}, headless={}",
                config.getMaxContexts(), config.isHeadless());
    }

    public List<AguiEvent> sendMessage(
            String userId,
            String message,
            HeadlessAgentListener listener) {
        return sendMessage(userId, message, listener, null);
    }

    public List<AguiEvent> sendMessage(
            String userId,
            String message,
            HeadlessAgentListener listener,
            HeadlessApprovalHandler approvalHandler) {

        HeadlessBrowserSession session = getOrCreateSession(userId);
        session.ensureHomePage(config.getBaseUrl());
        return session.sendMessage(message, listener, approvalHandler);
    }

    public HeadlessBrowserSession getOrCreateSession(String userId) {
        HeadlessBrowserSession existing = sessions.get(userId);
        if (existing != null) return existing;

        try {
            concurrencyLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取并发许可被中断", e);
        }

        try {
            final boolean[] created = {false};
            HeadlessBrowserSession session = sessions.computeIfAbsent(userId, id -> {
                created[0] = true;
                log.info("创建无头浏览器 Session: userId={}", id);
                BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
                        .setViewportSize(1920, 1080));
                return new HeadlessBrowserSession(id, ctx);
            });
            if (!created[0]) {
                concurrencyLimiter.release();
            }
            return session;
        } catch (Exception e) {
            concurrencyLimiter.release();
            throw e;
        }
    }

    public void closeSession(String userId) {
        HeadlessBrowserSession session = sessions.remove(userId);
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.warn("关闭 Session 异常: userId={}", userId, e);
            } finally {
                concurrencyLimiter.release();
            }
        }
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    @Override
    public void close() {
        log.info("HeadlessBrowserManager 关闭中...");
        idleChecker.shutdown();
        sessions.forEach((userId, session) -> {
            try { session.close(); } catch (Exception e) { log.warn("关闭异常: userId={}", userId, e); }
        });
        sessions.clear();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        log.info("HeadlessBrowserManager 已关闭");
    }

    private void checkIdleSessions() {
        long timeoutMs = config.getIdleTimeoutMs();
        for (Map.Entry<String, HeadlessBrowserSession> entry : sessions.entrySet()) {
            if (entry.getValue().isIdleTimeout(timeoutMs)) {
                log.info("Session 闲置超时，自动关闭: userId={}", entry.getKey());
                closeSession(entry.getKey());
            }
        }
    }
}
