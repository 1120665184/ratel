package org.quyq.gwsu.security.headless;

import io.agentscope.core.agui.event.AguiEvent;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.security.headless.config.HeadlessBrowserConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 无头浏览器管理器（分布式版本）
 * <p>
 * 职责：
 * 1. 管理 BrowserContextPool（预创建浏览器上下文池）
 * 2. 通过 Redis 存储分布式会话状态（token、threadId）
 * 3. 通过分布式锁防止同一用户并发访问
 * 4. 每次调用从池中借用 BrowserContext，用完销毁并补充
 * 5. 登录方式：有 session 时追加 token/threadId，无 session 时 certification 登录
 */
@Slf4j
public class HeadlessBrowserManager implements AutoCloseable {

    private final HeadlessBrowserConfiguration config;
    private final BrowserContextPool contextPool;
    private final CacheUtils cacheUtils;

    public HeadlessBrowserManager(HeadlessBrowserConfiguration config,
                                  BrowserContextPool contextPool,
                                  CacheUtils cacheUtils) {
        this.config = config;
        this.contextPool = contextPool;
        this.cacheUtils = cacheUtils;

        log.info("HeadlessBrowserManager 初始化完成: maxContexts={}, minIdle={}, headless={}, sessionTtlHours={}",
                config.getMaxContexts(), config.getMinIdle(), config.isHeadless(), config.getSessionTtlHours());
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

        String lockKey = "headless_lock:" + userId;
        return cacheUtils.executeWithLock(lockKey, () -> {
            HeadlessBrowserSession session = null;
            try {
                // 1. 从 Redis 读取分布式会话
                HeadlessAccessSession accessSession = getAccessSession(userId);

                // 2. 从池中借用 BrowserContext
                var ctx = contextPool.borrow(config.getBorrowTimeoutSeconds(), TimeUnit.SECONDS);

                // 3. 创建临时会话
                session = new HeadlessBrowserSession(ctx, config.getSseTimeoutMs(), cacheUtils);

                // 4. 构造登录 URL 并执行认证
                String loginUrl = buildLoginUrl(userId ,accessSession);
                try {
                    session.authenticate(loginUrl);
                } catch (Exception e) {
                    log.error("无头浏览器登录失败: userId={}", userId, e);
                    throw new RuntimeException("无头浏览器登录失败: userId=" + userId, e);
                }



                // 5. 发送消息
                List<AguiEvent> events = session.sendMessage(message, listener, approvalHandler);

                // 6. 从浏览器提取会话信息并保存到 Redis
                saveAccessSession(userId, session, accessSession);

                return events;
            } finally {
                // 7. 每次用完销毁 BrowserContext 和 Page，池会自动补充
                if (session != null) {
                    var ctx = session.getBrowserContext();
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.warn("关闭 Session 异常: userId={}", userId, e);
                    }
                    contextPool.returnAndReplenish(ctx);
                }
            }
        });
    }

    /**
     * 从 Redis 读取分布式会话
     */
    private HeadlessAccessSession getAccessSession(String userId) {
        String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + userId;
        return cacheUtils.withRebel(() -> {
            Map<String, String> map = cacheUtils.hGetAll(key, String.class);
            return HeadlessAccessSession.fromMap(map);
        });
    }

    /**
     * 从浏览器提取会话信息并保存到 Redis
     */
    private void saveAccessSession(String userId, HeadlessBrowserSession session, HeadlessAccessSession previousSession) {
        try {
            String token = session.extractTokenFromBrowser();
            String threadId = session.extractThreadId();

            // 合并：如果本次没有提取到某些值，保留之前的
            if (token == null && previousSession != null) token = previousSession.token();
            if (threadId == null && previousSession != null) threadId = previousSession.threadId();

            if (token == null) {
                log.warn("无法从浏览器提取 token，跳过保存 session: userId={}", userId);
                return;
            }

            HeadlessAccessSession newSession = new HeadlessAccessSession(userId, token, threadId);
            String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + userId;

            cacheUtils.withRebel(() -> {
                cacheUtils.hSetAll(key, newSession.toMap());
                cacheUtils.expire(key, config.getSessionTtlHours(), TimeUnit.HOURS);
                return null;
            });

            log.debug("保存分布式会话: userId={}, threadId={}", userId, threadId);
        } catch (Exception e) {
            log.warn("保存分布式会话失败: userId={}", userId, e);
        }
    }

    /**
     * 构造登录 URL
     * - 无 session: certification 登录
     * - 有 session: 追加 token 和 threadId
     */
    private String buildLoginUrl(String userId ,HeadlessAccessSession accessSession) {
        String certificationKey = UUID.randomUUID().toString();

        // 将凭证存入缓存，使用 withRebel 确保跨服务可读
        cacheUtils.withRebel(() -> {
            cacheUtils.set(
                    SecurityConstants.Authentication.HEADLESS_LOGIN_CERTIFICATION_CACHE_PREFIX + certificationKey,
                    userId,
                    2, TimeUnit.MINUTES
            );
            return null;
        });

        StringBuilder url = new StringBuilder(config.getLoginUrl());
        url.append("?certification=").append(certificationKey);

        if (accessSession != null && accessSession.token() != null) {
            url.append("&token=").append(accessSession.token());
            if (accessSession.threadId() != null) {
                url.append("&threadId=").append(accessSession.threadId());
            }
        }

        return url.toString();
    }

    /**
     * 删除用户的分布式会话
     */
    public void removeAccessSession(String userId) {
        String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + userId;
        cacheUtils.withRebel(() -> {
            cacheUtils.delete(key);
            return null;
        });
    }

    /**
     * 获取池中空闲数量
     */
    public int idleContextCount() {
        return contextPool.idleCount();
    }

    @Override
    public void close() {
        log.info("HeadlessBrowserManager 关闭中...");
        contextPool.close();
        log.info("HeadlessBrowserManager 已关闭");
    }
}
