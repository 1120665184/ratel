package org.quyq.gwsu.security.headless;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.*;
import io.agentscope.core.agui.event.AguiEvent;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.security.brain.push.AguiEventRedisPusher;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 无头浏览器用户会话（分布式版本）
 * <p>
 * 核心变更：
 * - 不再持有 userId 作为长期标识
 * - 每次使用时由 HeadlessBrowserManager 创建和销毁
 * - authenticate() 支持双模式：certification 登录 / token 快速恢复
 * - 支持从浏览器提取 token 和 threadId，保存到 Redis
 * <p>
 * SSE 事件接收策略：
 * 1. page.route() 拦截 agent/run 请求，从请求体中提取 threadId
 * 2. 先订阅 Redis channel（brain_sse_event_channel_{threadId}），再放行请求
 * 3. Redis 消息实时到达，反序列化为 AguiEvent 后分发给 listener
 * 4. 收到 RunFinished 事件后销毁 Redis 监听器
 */
@Slf4j
public class HeadlessBrowserSession implements AutoCloseable {

    private static final String SSE_URL_PATTERN = "/brain/run/copilotKit";

    private final BrowserContext context;
    private final Page page;
    private final HeadlessSseEventParser parser = new HeadlessSseEventParser();
    private final Gson gson = new Gson();
    private final CacheUtils cacheUtils;

    /** SSE 等待超时（毫秒），从配置注入 */
    private final long sseTimeoutMs;

    /** sendMessage 互斥锁，保证同一 Session 不会并发调用 */
    private final ReentrantLock sendLock = new ReentrantLock();

    /** UI 操作线程池 */
    private ExecutorService uiExecutor;

    /** 当前活跃的事件收集器（每次 sendMessage 重建） */
    private final AtomicReference<SseEventCollector> currentEventCollector = new AtomicReference<>();

    /** 当前活跃的监听器和处理器（每次 sendMessage 更新） */
    private final AtomicReference<HeadlessAgentListener> currentListener = new AtomicReference<>();
    private final AtomicReference<HeadlessApprovalHandler> currentApprovalHandler = new AtomicReference<>();

    /** toolCallId → toolCallName 映射 */
    private final ConcurrentHashMap<String, String> toolCallNameMap = new ConcurrentHashMap<>();
    /** toolCallId → 累积的 TOOL_CALL_ARGS delta */
    private final ConcurrentHashMap<String, StringBuilder> toolCallArgsBuffer = new ConcurrentHashMap<>();

    private final AtomicReference<CompletableFuture<HeadlessApprovalHandler.ApprovalResult>> approvalFuture = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<Map<String, String>>> askQuestionFuture = new AtomicReference<>();

    /** 当前 Redis 监听容器，收到 RunFinished 后销毁 */
    private volatile RedisMessageListenerContainer currentRedisListener = null;

    /** 当前 SSE 会话的 threadId，从拦截的请求中提取 */
    private volatile String currentThreadId = null;

    private volatile boolean closed = false;

    HeadlessBrowserSession(BrowserContext context, long sseTimeoutMs, CacheUtils cacheUtils) {
        this.context = context;
        this.sseTimeoutMs = sseTimeoutMs;
        this.cacheUtils = cacheUtils;

        // 1. 创建 Page
        this.page = context.newPage();

        // 2. page.route() 拦截 SSE 请求，提取 threadId 并订阅 Redis
        page.route("**" + SSE_URL_PATTERN + "**", route -> {
            String postData = route.request().postData();
            boolean isSse = postData != null && postData.contains("agent/run");

            if (isSse) {
                String threadId = extractThreadId(postData);
                if (threadId != null) {
                    this.currentThreadId = threadId;
                    log.info("[HeadlessSSE] 检测到agent/run请求, threadId={}", threadId);
                    subscribeRedisEvents(threadId);
                } else {
                    log.warn("[HeadlessSSE] 无法从请求体中提取threadId");
                }
                route.fallback();
            } else {
                route.fallback();
            }
        });

        // 3. 转发浏览器控制台日志（调试用）
        page.onConsoleMessage(msg -> {
            String text = msg.text();
            if (text != null && text.startsWith("[Headless")) {
                log.info("[BrowserConsole] {}", text);
            }
        });

        this.uiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "headless-ui-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 获取 BrowserContext，供 Manager 归还到池
     */
    public BrowserContext getBrowserContext() {
        return context;
    }

    /**
     * 从浏览器 localStorage 提取 token
     */
    public String extractTokenFromBrowser() {
        try {
            Object tokenObj = page.evaluate(
                    "() => { try { const raw = localStorage.getItem('gwsu_token'); if (!raw) return null; const parsed = JSON.parse(raw); return parsed.token || null; } catch { return null; } }"
            );
            return tokenObj != null ? tokenObj.toString() : null;
        } catch (Exception e) {
            log.debug("从浏览器提取 token 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前 SSE 会话的 threadId
     */
    public String extractThreadId() {
        return currentThreadId;
    }

    /**
     * 认证登录
     * <p>
     * 流程：
     * 1. URL 含 token 参数 → 先导航到带 token 的登录页面
     *    - 通过 page.onResponse 监听网络请求，检测 /system/manager/current 返回 401
     *    - 如果 401 → 清除无效 token，重新导航到只有 certification 的 URL 回退登录
     *    - 如果正常 → 等待 data-headless-login-status 变为 success
     * 2. URL 无 token 参数 → 直接 certification 登录
     */
    void authenticate(String loginUrl) {
        log.info("开始无头浏览器认证: loginUrl={}", loginUrl);

        boolean hasToken = loginUrl.contains("token=");

        if (hasToken) {
            // 有 token：先尝试 token 恢复，如果 401 则回退 certification
            authenticateWithTokenFallback(loginUrl);
        } else {
            // 无 token：直接 certification 登录
            authenticateDirect(loginUrl);
        }
    }

    /**
     * 带 token 的认证流程，检测 401 后自动回退 certification 登录
     */
    private void authenticateWithTokenFallback(String loginUrl) {
        log.info("检测到 token 参数，尝试 token 恢复会话");

        // 注册响应监听器，检测 fetchCurrentUserInfo 返回 401
        final boolean[] tokenExpired = {false};
        java.util.function.Consumer<Response> responseListener = response -> {
            String rUrl = response.url();
            // 检测获取当前用户信息的接口返回 401
            if (rUrl.contains("/%s/manager/current".formatted(SecurityConstants.Authentication.AUTH_SERVER_PREFIX)) && response.status() == 401) {
                log.warn("[HeadlessAuth] 检测到 token 失效 (401): {}", rUrl);
                tokenExpired[0] = true;
            }
        };
        page.onResponse(responseListener);

        try {
            // 导航到带 token 的登录页面
            page.navigate(loginUrl);
            page.waitForLoadState();

            // 等待前端设置 data-headless-login-status 属性
            page.waitForFunction(
                    "() => document.body && document.body.getAttribute('data-headless-login-status') !== null",
                    null, new Page.WaitForFunctionOptions().setTimeout(30_000));

            // 检查是否 token 失效
            if (tokenExpired[0]) {
                log.info("[HeadlessAuth] token 已失效，回退到 certification 登录");
                // 清除浏览器中无效的 token
                page.evaluate("() => { localStorage.removeItem('gwsu_token'); localStorage.removeItem('gwsu_isLoggedIn'); }");
                // 构造移除 token 但保留 threadId 的 URL
                String fallbackUrl = stripTokenParam(loginUrl);
                // 回退到 certification 登录
                authenticateDirect(fallbackUrl);
                return;
            }

            // token 有效，检查登录状态
            Object statusObj = page.evaluate("document.body.getAttribute('data-headless-login-status')");
            String status = statusObj != null ? statusObj.toString() : "";
            if ("success".equals(status)) {
                log.info("token 恢复会话成功");
                return;
            }

            // 其他错误状态（非 401），也回退 certification
            log.warn("[HeadlessAuth] 登录状态异常(status={}), 回退到 certification 登录", status);
            page.evaluate("() => { localStorage.removeItem('gwsu_token'); localStorage.removeItem('gwsu_isLoggedIn'); }");
            String fallbackUrl = stripTokenParam(loginUrl);
            authenticateDirect(fallbackUrl);

        } catch (PlaywrightException e) {
            // 超时也可能是因为 token 失效导致页面跳转
            if (tokenExpired[0]) {
                log.info("[HeadlessAuth] 超时且 token 已失效，回退到 certification 登录");
                try {
                    page.evaluate("() => { localStorage.removeItem('gwsu_token'); localStorage.removeItem('gwsu_isLoggedIn'); }");
                } catch (Exception ignored) {}
                String fallbackUrl = stripTokenParam(loginUrl);
                authenticateDirect(fallbackUrl);
            } else {
                log.error("无头浏览器 token 恢复超时", e);
                throw new RuntimeException("无头浏览器登录超时", e);
            }
        } finally {
            // 移除响应监听器（避免影响后续请求）
            try { page.offResponse(responseListener); } catch (Exception ignored) {}
        }
    }

    /**
     * 直接 certification 登录（无 token 参数）
     */
    private void authenticateDirect(String loginUrl) {
        log.info("开始 certification 登录: loginUrl={}", loginUrl);

        page.navigate(loginUrl);
        page.waitForLoadState();

        try {
            page.waitForFunction(
                    "() => document.body && document.body.getAttribute('data-headless-login-status') !== null",
                    null, new Page.WaitForFunctionOptions().setTimeout(30_000));
        } catch (PlaywrightException e) {
            log.error("无头浏览器 certification 登录超时", e);
            throw new RuntimeException("无头浏览器登录超时", e);
        }

        Object statusObj = page.evaluate("document.body.getAttribute('data-headless-login-status')");
        String status = statusObj != null ? statusObj.toString() : "";
        if (!"success".equals(status)) {
            log.error("无头浏览器 certification 登录失败: status={}", status);
            throw new RuntimeException("无头浏览器登录失败: " + status);
        }
        log.info("certification 登录成功");
    }

    /**
     * 从 URL 中移除 token 参数，保留 certification 和 threadId
     */
    private String stripTokenParam(String url) {
        try {
            var uri = new java.net.URI(url);
            String query = uri.getQuery();
            if (query == null) return url;

            StringBuilder newQuery = new StringBuilder();
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && !"token".equals(kv[0])) {
                    if (newQuery.length() > 0) newQuery.append("&");
                    newQuery.append(param);
                }
            }

            return new java.net.URI(uri.getScheme(), uri.getAuthority(), uri.getPath(),
                    !newQuery.isEmpty() ? newQuery.toString() : null, uri.getFragment()).toString();
        } catch (Exception e) {
            // 解析失败，返回原 URL
            log.warn("解析 URL 失败，返回原 URL: {}", e.getMessage());
            return url;
        }
    }

    public List<AguiEvent> sendMessage(
            String message,
            HeadlessAgentListener listener,
            HeadlessApprovalHandler approvalHandler) {

        sendLock.lock();
        try {
            toolCallNameMap.clear();
            toolCallArgsBuffer.clear();
            cancelPendingFutures();

            SseEventCollector collector = new SseEventCollector();
            currentEventCollector.set(collector);
            currentListener.set(listener);
            currentApprovalHandler.set(approvalHandler);

            try {
                triggerAssistant(message);
            } catch (Exception e) {
                collector.signalError(e);
                notifyListenerError(e);
                return collector.getEvents();
            }

            collector.awaitCompletion(sseTimeoutMs);
            return collector.getEvents();
        } finally {
            sendLock.unlock();
        }
    }

    public List<AguiEvent> sendMessage(String message, HeadlessAgentListener listener) {
        return sendMessage(message, listener, null);
    }

    public void approve() { approve(null); }

    public void approve(HeadlessApprovalHandler.ApprovalResult result) {
        CompletableFuture<HeadlessApprovalHandler.ApprovalResult> future = approvalFuture.get();
        if (future != null) future.complete(result != null ? result : HeadlessApprovalHandler.ApprovalResult.accept());
    }

    public void reject(String reason) {
        CompletableFuture<HeadlessApprovalHandler.ApprovalResult> future = approvalFuture.get();
        if (future != null) future.complete(HeadlessApprovalHandler.ApprovalResult.reject(reason));
    }

    public void answerQuestion(Map<String, String> answers) {
        CompletableFuture<Map<String, String>> future = askQuestionFuture.get();
        if (future != null) future.complete(answers);
    }

    public void ensureHomePage(String baseUrl) {
        String currentUrl = page.url();
        if (currentUrl == null || currentUrl.isEmpty() || currentUrl.equals("about:blank")) {
            page.navigate(baseUrl);
            page.waitForLoadState();
        }
    }

    @Override
    public void close() {
        closed = true;
        cancelPendingFutures();
        destroyRedisListener();
        toolCallNameMap.clear();
        toolCallArgsBuffer.clear();
        if (uiExecutor != null) uiExecutor.shutdownNow();
        try { if (page != null && !page.isClosed()) page.close(); } catch (Exception e) { log.warn("关闭Page异常", e); }
        try { if (context != null) context.close(); } catch (Exception e) { log.warn("关闭Context异常", e); }
    }

    // ==================== 内部实现 ====================

    private void cancelPendingFutures() {
        CompletableFuture<HeadlessApprovalHandler.ApprovalResult> af = approvalFuture.getAndSet(null);
        if (af != null) af.cancel(true);
        CompletableFuture<Map<String, String>> aqf = askQuestionFuture.getAndSet(null);
        if (aqf != null) aqf.cancel(true);
    }

    private void notifyListenerError(Throwable error) {
        HeadlessAgentListener listener = currentListener.get();
        if (listener != null) {
            try { listener.onError(error); } catch (Exception e) { log.warn("通知监听器错误失败", e); }
        }
    }

    /**
     * 从请求体 JSON 中提取 threadId
     */
    private String extractThreadId(String postData) {
        try {
            Map<String, Object> map = gson.fromJson(postData, Map.class);
            Object body = map.get("body");
            if (body instanceof Map<?, ?> bodyMap) {
                Object threadId = bodyMap.get("threadId");
                if (threadId != null) return threadId.toString();
            }
            Object threadId = map.get("threadId");
            if (threadId != null) return threadId.toString();
        } catch (Exception e) {
            log.warn("[HeadlessSSE] 解析threadId失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 订阅 Redis channel 接收 SSE 事件
     */
    private void subscribeRedisEvents(String threadId) {
        destroyRedisListener();

        String channel = AguiEventRedisPusher.BRAIN_SSE_EVENT_CHANNEL_PREFIX + threadId;
        log.info("[HeadlessSSE] 订阅Redis channel: {}", channel);

        MessageListener listener = (message, pattern) -> {
            try {
                String msg = (String) cacheUtils.getSerializer().deserialize(message.getBody());
                AguiEvent event = parser.parseEvent(msg);
                if (event != null) {
                    handleSseEvent(event);
                    if (event instanceof AguiEvent.RunFinished) {
                        log.info("[HeadlessSSE] 收到RunFinished, 销毁Redis监听器: threadId={}", threadId);
                        destroyRedisListener();
                    }
                }
            } catch (Exception e) {
                log.warn("[HeadlessSSE] Redis消息处理异常: {}", e.getMessage(), e);
            }
        };

        currentRedisListener = cacheUtils.withRebel(() -> cacheUtils.addListener(channel, listener));
        log.info("[HeadlessSSE] Redis监听器已就绪: threadId={}", threadId);
    }

    /**
     * 销毁当前 Redis 监听器
     */
    private void destroyRedisListener() {
        RedisMessageListenerContainer listener = currentRedisListener;
        if (listener != null) {
            currentRedisListener = null;
            try {
                listener.stop();
                listener.destroy();
            } catch (Exception e) {
                log.warn("[HeadlessSSE] 销毁Redis监听器异常: {}", e.getMessage());
            }
        }
    }

    private void handleSseEvent(AguiEvent event) {
        SseEventCollector collector = currentEventCollector.get();
        HeadlessAgentListener listener = currentListener.get();
        if (collector != null) collector.addEvent(event);
        if (listener == null) return;

        listener.onEvent(event);

        switch (event) {
            case AguiEvent.RunStarted e -> listener.onRunStarted(e);
            case AguiEvent.RunFinished e -> listener.onRunFinished(e);
            case AguiEvent.TextMessageStart e -> listener.onTextMessageStart(e);
            case AguiEvent.TextMessageContent e -> listener.onTextMessageContent(e.delta());
            case AguiEvent.TextMessageEnd e -> listener.onTextMessageEnd(e);
            case AguiEvent.ToolCallStart e -> {
                listener.onToolCallStart(e);
                toolCallNameMap.put(e.toolCallId(), e.toolCallName());
                if ("AskUserQuestion".equals(e.toolCallName())) toolCallArgsBuffer.put(e.toolCallId(), new StringBuilder());
            }
            case AguiEvent.ToolCallArgs e -> {
                listener.onToolCallArgs(e);
                StringBuilder argsBuf = toolCallArgsBuffer.get(e.toolCallId());
                if (argsBuf != null) argsBuf.append(e.delta());
            }
            case AguiEvent.ToolCallEnd e -> {
                listener.onToolCallEnd(e);
                if ("AskUserQuestion".equals(toolCallNameMap.get(e.toolCallId()))) handleAskUserQuestionEvent(e.toolCallId());
                toolCallNameMap.remove(e.toolCallId());
                toolCallArgsBuffer.remove(e.toolCallId());
            }
            case AguiEvent.ToolCallResult e -> listener.onToolCallResult(e);
            case AguiEvent.StateSnapshot e -> listener.onStateSnapshot(e);
            case AguiEvent.StateDelta e -> listener.onStateDelta(e);
            case AguiEvent.Custom e -> {
                String name = e.name();
                if ("HUMAN_APPROVAL".equals(name)) { listener.onHumanApproval(e); handleHumanApprovalEvent(e); }
                else if ("TOOL_EXECUTE".equals(name)) listener.onToolExecute(e);
                else if ("AGENT_OUTPUT".equals(name) || "AGENT_OUTPUT_END".equals(name)) listener.onAgentOutput(e);
                else listener.onCustomEvent(e);
            }
            default -> { }
        }
    }

    private void handleHumanApprovalEvent(AguiEvent.Custom event) {
        HeadlessApprovalHandler handler = currentApprovalHandler.get();
        if (handler != null) {
            submitUiTask(() -> operateApprovalUI(handler.handleApproval(event)));
        } else {
            final var approvalFuture = new CompletableFuture<HeadlessApprovalHandler.ApprovalResult>();
            this.approvalFuture.set(approvalFuture);
            submitUiTask(() -> {
                try {
                    operateApprovalUI(approvalFuture.get(sseTimeoutMs, TimeUnit.MILLISECONDS));
                } catch (TimeoutException e) { notifyListenerError(new TimeoutException("审批等待超时")); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { notifyListenerError(e.getCause()); }
                finally { HeadlessBrowserSession.this.approvalFuture.compareAndSet(approvalFuture, null); }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void handleAskUserQuestionEvent(String toolCallId) {
        StringBuilder argsBuf = toolCallArgsBuffer.get(toolCallId);
        Map<String, Object> questions;
        if (argsBuf != null && !argsBuf.isEmpty()) {
            Map<String, Object> parsed = null;
            try { parsed = gson.fromJson(argsBuf.toString(), Map.class); } catch (Exception e) { log.warn("AskUserQuestion参数解析失败: {}", toolCallId, e); }
            questions = parsed != null ? parsed : Map.of();
        } else {
            questions = Map.of();
        }
        HeadlessAgentListener listener = currentListener.get();
        if (listener != null) listener.onAskUserQuestion(toolCallId, questions);
        HeadlessApprovalHandler handler = currentApprovalHandler.get();
        if (handler != null) {
            submitUiTask(() -> operateAskUserQuestionUI(handler.handleAskUserQuestion(toolCallId, questions)));
        } else {
            final var askFuture = new CompletableFuture<Map<String, String>>();
            askQuestionFuture.set(askFuture);
            submitUiTask(() -> {
                try { operateAskUserQuestionUI(askFuture.get(sseTimeoutMs, TimeUnit.MILLISECONDS)); }
                catch (TimeoutException e) { notifyListenerError(new TimeoutException("提问等待超时")); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { notifyListenerError(e.getCause()); }
                finally { askQuestionFuture.compareAndSet(askFuture, null); }
            });
        }
    }

    private void submitUiTask(Runnable task) {
        if (closed) return;
        uiExecutor.submit(() -> { try { task.run(); } catch (Exception e) { log.error("UI操作执行失败", e); } });
    }

    private void operateApprovalUI(HeadlessApprovalHandler.ApprovalResult result) {
        try {
            if (result.approved()) page.locator("[data-testid='btn-approve']").click();
            else {
                page.locator("[data-testid='btn-reject']").click();
                if (result.rejectReason() != null && !result.rejectReason().isEmpty()) {
                    page.locator("[data-testid='input-reject-reason']").fill(result.rejectReason());
                    page.locator("[data-testid='btn-submit-reject']").click();
                }
            }
        } catch (Exception e) { log.error("审批界面操作失败", e); }
    }

    private void operateAskUserQuestionUI(Map<String, String> answers) {
        try {
            for (Map.Entry<String, String> entry : answers.entrySet())
                page.locator(String.format("[data-testid='option-item']:has-text('%s')", entry.getValue())).click();
            page.locator("[data-testid='btn-submit-answer']").click();
        } catch (Exception e) { log.error("提问界面操作失败", e); }
    }

    private void triggerAssistant(String message) {
        try {
            // 等待前端聊天就绪（历史消息回显完成）
            page.waitForFunction(
                    "() => document.body.getAttribute('data-headless-chat-ready') === 'true'",
                    null, new Page.WaitForFunctionOptions().setTimeout(30_000));
            log.debug("前端聊天已就绪，开始发送消息");

            page.locator("[data-testid='copilot-chat-textarea']").fill(message);
            page.locator("[data-testid='copilot-send-button']").click();
            log.debug("助手消息已发送: message={}", message);
        } catch (Exception e) {
            log.error("触发助手失败", e);
            throw new RuntimeException("触发助手失败", e);
        }
    }

    // ==================== 事件收集器 ====================

    static class SseEventCollector {
        private final List<AguiEvent> events = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch completionLatch = new CountDownLatch(1);
        private volatile Throwable error;

        void addEvent(AguiEvent event) {
            events.add(event);
            if (event instanceof AguiEvent.RunFinished) completionLatch.countDown();
        }
        void signalError(Throwable t) { this.error = t; completionLatch.countDown(); }
        List<AguiEvent> awaitCompletion(long timeoutMs) {
            try { completionLatch.await(timeoutMs, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return getEvents();
        }
        List<AguiEvent> getEvents() { return List.copyOf(events); }
        Throwable getError() { return error; }
    }
}
