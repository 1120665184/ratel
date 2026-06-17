package org.quyq.gwsu.security.headless;

import io.agentscope.core.agui.event.AguiEvent;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.security.config.HeadlessBrowserConfiguration;
import org.quyq.gwsu.security.headless.pool.BrowserContextPool;
import org.quyq.gwsu.security.headless.session.HeadlessAccessSession;
import org.quyq.gwsu.security.headless.session.HeadlessBrowserSession;

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
 * <p>
 * 交互方式：
 * - sendMessage：发送普通聊天消息，SSE 流实时推送事件给 listener
 * - approval：提交人工审批结果，通过前端隐藏表单提交
 * - userAnswer：提交用户问题回答，通过前端隐藏表单提交
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

    /**
     * 发送聊天消息
     * <p>
     * 流程：获取分布式锁 → 借用 BrowserContext → 认证登录 → 发送消息 → 收集 SSE 事件 → 保存会话
     * <p>
     * 当 SSE 流中出现 HUMAN_APPROVAL 或 AskUserQuestion 事件时，流会结束，
     * 调用方需根据 listener 回调决定后续操作（调用 approval() 或 userAnswer()）
     *
     * @param userId   用户 ID
     * @param message  消息内容
     * @param listener 事件监听器
     * @return SSE 事件列表
     */
    public List<AguiEvent> sendMessage(String userId, String message, HeadlessAgentListener listener) {
        String lockKey = "headless_lock:" + userId;
        return cacheUtils.executeWithLock(lockKey, () ->
                executeWithSession(userId, "发送消息", session -> {
                    List<AguiEvent> events = session.sendMessage(message, listener);
                    return events;
                })
        );
    }

    /**
     * 提交人工审批结果
     * <p>
     * 审批时 SSE 流已断开，此方法独立发起操作：
     * 登录恢复会话 → 等待聊天就绪 → 填充审批隐藏表单 → 点击提交 → 监听后续 SSE 事件
     *
     * @param userId       用户 ID
     * @param approved     是否批准
     * @param rejectReason 拒绝原因（批准时可为 null）
     * @param listener     事件监听器，接收提交后的 SSE 事件
     */
    public void approval(String userId, boolean approved, String rejectReason, HeadlessAgentListener listener) {
        String lockKey = "headless_lock:" + userId;
        cacheUtils.executeWithLock(lockKey, () ->
                executeWithSession(userId, "审批", session -> {
                    session.submitApproval(approved, rejectReason, listener);
                    return null;
                })
        );
    }

    /**
     * 提交用户问题回答
     * <p>
     * 回答问题时 SSE 流已断开，此方法独立发起操作：
     * 登录恢复会话 → 等待聊天就绪 → 填充回答隐藏表单 → 点击提交 → 监听后续 SSE 事件
     *
     * @param userId     用户 ID
     * @param toolCallId 工具调用 ID，用于关联 AskUserQuestion 工具调用
     * @param answers    问题答案，key 为问题文本，value 为用户回答
     * @param listener   事件监听器，接收提交后的 SSE 事件
     */
    public void userAnswer(String userId, String toolCallId, Map<String, String> answers, HeadlessAgentListener listener) {
        String lockKey = "headless_lock:" + userId;
        cacheUtils.executeWithLock(lockKey, () ->
                executeWithSession(userId, "回答问题", session -> {
                    session.submitUserAnswer(toolCallId, answers, listener);
                    return null;
                })
        );
    }

    // ==================== 公共会话执行模板 ====================

    /**
     * 公共会话执行模板
     * <p>
     * 统一处理：获取分布式会话 → 借用 BrowserContext → 创建 Session → 认证 → 执行操作 → 保存会话 → 归还资源
     * sendMessage / approval / userAnswer 的区别仅在于 action 中的操作不同
     *
     * @param userId   用户 ID
     * @param actionName 操作名称（用于日志）
     * @param action  会话操作回调
     * @return 操作结果
     */
    private <T> T executeWithSession(String userId, String actionName, SessionAction<T> action) {
        HeadlessBrowserSession session = null;
        try {
            // 1. 从 Redis 读取分布式会话
            HeadlessAccessSession accessSession = getAccessSession(userId);

            // 2. 从池中借用 BrowserContext
            var ctx = contextPool.borrow(config.getBorrowTimeoutSeconds(), TimeUnit.SECONDS);

            // 3. 创建临时会话
            session = new HeadlessBrowserSession(ctx, config.getSseTimeoutMs(), cacheUtils);

            // 4. 构造登录 URL 并执行认证
            String loginUrl = buildLoginUrl(userId, accessSession);
            try {
                session.authenticate(loginUrl);
            } catch (Exception e) {
                log.error("{}登录失败: userId={}", actionName, userId, e);
                throw new RuntimeException(actionName + "登录失败: userId=" + userId, e);
            }

            // 5. 执行具体操作
            T result = action.execute(session);

            // 6. 从浏览器提取会话信息并保存到 Redis
            saveAccessSession(userId, session, accessSession);

            return result;
        } finally {
            // 7. 归还资源
            if (session != null) {
                var ctx = session.getBrowserContext();
                try { session.close(); } catch (Exception e) { log.warn("关闭 Session 异常: userId={}", userId, e); }
                contextPool.returnAndReplenish(ctx);
            }
        }
    }

    /**
     * 会话操作回调接口
     */
    @FunctionalInterface
    private interface SessionAction<T> {
        T execute(HeadlessBrowserSession session);
    }

    // ==================== 分布式会话管理 ====================

    /**
     * 从 Redis 读取分布式会话
     */
    public HeadlessAccessSession getAccessSession(String userId) {
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
    private String buildLoginUrl(String userId, HeadlessAccessSession accessSession) {
        String certificationKey = UUID.randomUUID().toString();

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
     * 开启新会话
     * <p>
     * 清除 Redis 中的 threadId，保留 token。下次 sendMessage 时会以已有 token 登录，
     * 但不携带 threadId，从而创建新的聊天线程。
     *
     * @param userId 用户 ID
     */
    public void newSession(String userId) {
        String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + userId;
        cacheUtils.withRebel(() -> {
            cacheUtils.hDelete(key, HeadlessAccessSession.HASH_KEY_THREAD_ID);
            return null;
        });
        log.debug("已清除用户 threadId，下次将创建新会话: userId={}", userId);
    }

    public void newSession(String userId , String threadId) {
        String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + userId;
        cacheUtils.withRebel(() -> {
            cacheUtils.hSet(key , HeadlessAccessSession.HASH_KEY_THREAD_ID , threadId);
            return null;
        });
        log.debug("已设置新会话threadID，下次将使用设置值: userId={}", userId);
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
