package org.quyq.gwsu.security.headless.session;

import com.google.gson.Gson;
import com.microsoft.playwright.*;
import io.agentscope.core.agui.event.AguiEvent;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.security.brain.push.AguiEventRedisPusher;
import org.quyq.gwsu.security.headless.HeadlessAgentListener;
import org.quyq.gwsu.security.headless.parser.HeadlessSseEventParser;
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
 * <p>
 * 审批/提问交互策略：
 * - 审批和回答问题由 HeadlessBrowserManager 的 approval()/userAnswer() 独立发起
 * - 通过前端隐藏表单提交，无需操作可见 UI 元素
 * - submitApproval()/submitUserAnswer() 填充隐藏表单并触发提交
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

    /** 当前活跃的事件收集器（每次 sendMessage 重建） */
    private final AtomicReference<SseEventCollector> currentEventCollector = new AtomicReference<>();

    /** 当前活跃的监听器（每次 sendMessage 更新） */
    private final AtomicReference<HeadlessAgentListener> currentListener = new AtomicReference<>();

    /** 页面操作包装器（供 listener 在事件回调中操作浏览器） */
    private final HeadlessPageWrapper pageWrapper;

    /** toolCallId → toolCallName 映射 */
    private final ConcurrentHashMap<String, String> toolCallNameMap = new ConcurrentHashMap<>();
    /** toolCallId → 累积的 TOOL_CALL_ARGS delta */
    private final ConcurrentHashMap<String, StringBuilder> toolCallArgsBuffer = new ConcurrentHashMap<>();

    /** 当前 Redis 监听容器，收到 RunFinished 后销毁 */
    private volatile RedisMessageListenerContainer currentRedisListener = null;

    /** 当前 SSE 会话的 threadId，从拦截的请求中提取 */
    private volatile String currentThreadId = null;

    private volatile boolean closed = false;

    public HeadlessBrowserSession(BrowserContext context, long sseTimeoutMs, CacheUtils cacheUtils) {
        this.context = context;
        this.sseTimeoutMs = sseTimeoutMs;
        this.cacheUtils = cacheUtils;

        // 1. 创建 Page
        this.page = context.newPage();

        // 2. 创建页面操作包装器
        this.pageWrapper = new HeadlessPageWrapper(context, page);

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
    public void authenticate(String loginUrl) {
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

    // ==================== 发送消息 ====================

    public List<AguiEvent> sendMessage(String message, HeadlessAgentListener listener) {
        sendLock.lock();
        try {
            toolCallNameMap.clear();
            toolCallArgsBuffer.clear();

            SseEventCollector collector = new SseEventCollector();
            currentEventCollector.set(collector);
            currentListener.set(listener);

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

    // ==================== 审批/回答问题（通过隐藏表单提交） ====================

    /**
     * 通过隐藏表单提交审批结果，并监听后续 SSE 事件
     * <p>
     * 流程：等待聊天就绪 → 填充隐藏表单 → 触发提交 → 等待 SSE 流完成
     *
     * @param approved     是否批准
     * @param rejectReason 拒绝原因（批准时为 null）
     * @param listener     事件监听器，接收提交后的 SSE 事件
     */
    public void submitApproval(boolean approved, String rejectReason, HeadlessAgentListener listener) {
        sendLock.lock();
        try {
            toolCallNameMap.clear();
            toolCallArgsBuffer.clear();

            SseEventCollector collector = new SseEventCollector();
            currentEventCollector.set(collector);
            currentListener.set(listener);

            // 等待前端聊天就绪
            page.waitForFunction(
                    "() => document.body.getAttribute('data-headless-chat-ready') === 'true'",
                    null, new Page.WaitForFunctionOptions().setTimeout(30_000));
            log.debug("前端聊天已就绪，开始提交审批");

            String result = approved ? "APPROVED" : "REJECTED";
            String reason = rejectReason != null ? rejectReason : "";

            // 填充隐藏表单并触发提交，同时等待 agent run 请求发送
            try {
                page.waitForRequest(
                        req -> req.url().contains(SSE_URL_PATTERN),
                        () -> page.evaluate("args => {" +
                                "  var r = document.querySelector('[data-testid=\"headless-approval-result\"]');" +
                                "  var re = document.querySelector('[data-testid=\"headless-approval-reject-reason\"]');" +
                                "  if (r) r.value = args[0];" +
                                "  if (re) re.value = args[1];" +
                                "  var btn = document.querySelector('[data-testid=\"headless-approval-submit\"]');" +
                                "  if (btn) btn.click();" +
                                "}", new Object[]{ result, reason })
                );
            } catch (PlaywrightException e) {
                log.warn("等待审批请求发送超时，审批可能已提交");
            }

            log.info("审批结果已提交: result={}, hasRejectReason={}", result, !reason.isEmpty());

            // 等待后续 SSE 流完成
            collector.awaitCompletion(sseTimeoutMs);
        } catch (Exception e) {
            log.error("提交审批失败", e);
            throw new RuntimeException("提交审批失败", e);
        } finally {
            sendLock.unlock();
        }
    }

    /**
     * 通过隐藏表单提交用户回答，并监听后续 SSE 事件
     * <p>
     * 流程：等待聊天就绪 → 填充隐藏表单（answers + toolCallId）→ 触发提交 → 等待 SSE 流完成
     *
     * @param toolCallId 工具调用 ID，用于关联 AskUserQuestion 工具调用
     * @param answers    问题答案，key 为问题文本，value 为用户回答
     * @param listener   事件监听器，接收提交后的 SSE 事件
     */
    public void submitUserAnswer(String toolCallId, Map<String, String> answers, HeadlessAgentListener listener) {
        sendLock.lock();
        try {
            toolCallNameMap.clear();
            toolCallArgsBuffer.clear();

            SseEventCollector collector = new SseEventCollector();
            currentEventCollector.set(collector);
            currentListener.set(listener);

            // 等待前端聊天就绪
            page.waitForFunction(
                    "() => document.body.getAttribute('data-headless-chat-ready') === 'true'",
                    null, new Page.WaitForFunctionOptions().setTimeout(30_000));
            log.debug("前端聊天已就绪，开始提交用户回答");

            String answersJson = gson.toJson(answers);

            // 填充隐藏表单并触发提交，同时等待 agent run 请求发送
            try {
                page.waitForRequest(
                        req -> req.url().contains(SSE_URL_PATTERN),
                        () -> page.evaluate("args => {" +
                                "  var a = document.querySelector('[data-testid=\"headless-question-answers\"]');" +
                                "  var t = document.querySelector('[data-testid=\"headless-question-tool-call-id\"]');" +
                                "  if (a) a.value = args[0];" +
                                "  if (t) t.value = args[1];" +
                                "  var btn = document.querySelector('[data-testid=\"headless-question-submit\"]');" +
                                "  if (btn) btn.click();" +
                                "}", new Object[]{ answersJson, toolCallId })
                );
            } catch (PlaywrightException e) {
                log.warn("等待回答问题请求发送超时，回答可能已提交");
            }

            log.info("用户回答已提交: toolCallId={}", toolCallId);

            // 等待后续 SSE 流完成
            collector.awaitCompletion(sseTimeoutMs);
        } catch (Exception e) {
            log.error("提交用户回答失败", e);
            throw new RuntimeException("提交用户回答失败", e);
        } finally {
            sendLock.unlock();
        }
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
        destroyRedisListener();
        pageWrapper.markClosed();
        toolCallNameMap.clear();
        toolCallArgsBuffer.clear();
        try { if (page != null && !page.isClosed()) page.close(); } catch (Exception e) { log.warn("关闭Page异常", e); }
        try { if (context != null) context.close(); } catch (Exception e) { log.warn("关闭Context异常", e); }
    }

    // ==================== 内部实现 ====================

    private void notifyListenerError(Throwable error) {
        HeadlessAgentListener listener = currentListener.get();
        if (listener != null) {
            try { listener.onError(error, pageWrapper); } catch (Exception e) { log.warn("通知监听器错误失败", e); }
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
        if (closed) {
            log.debug("[HeadlessSSE] Session已关闭，忽略事件: {}", event.getClass().getSimpleName());
            return;
        }
        SseEventCollector collector = currentEventCollector.get();
        HeadlessAgentListener listener = currentListener.get();
        if (collector != null) collector.addEvent(event);
        if (listener == null) return;

        try {
            listener.onEvent(event, pageWrapper);

            switch (event) {
                case AguiEvent.RunStarted e -> listener.onRunStarted(e, pageWrapper);
                case AguiEvent.RunFinished e -> listener.onRunFinished(e, pageWrapper);
                case AguiEvent.TextMessageStart e -> listener.onTextMessageStart(e, pageWrapper);
                case AguiEvent.TextMessageContent e -> listener.onTextMessageContent(e.delta(), pageWrapper);
                case AguiEvent.TextMessageEnd e -> listener.onTextMessageEnd(e, pageWrapper);
                case AguiEvent.ToolCallStart e -> {
                    listener.onToolCallStart(e, pageWrapper);
                    toolCallNameMap.put(e.toolCallId(), e.toolCallName());
                    if ("AskUserQuestion".equals(e.toolCallName())) toolCallArgsBuffer.put(e.toolCallId(), new StringBuilder());
                }
                case AguiEvent.ToolCallArgs e -> {
                    listener.onToolCallArgs(e, pageWrapper);
                    StringBuilder argsBuf = toolCallArgsBuffer.get(e.toolCallId());
                    if (argsBuf != null) argsBuf.append(e.delta());
                }
                case AguiEvent.ToolCallEnd e -> {
                    listener.onToolCallEnd(e, pageWrapper);
                    if ("AskUserQuestion".equals(toolCallNameMap.get(e.toolCallId()))) {
                        notifyAskUserQuestion(e.toolCallId());
                    }
                    toolCallNameMap.remove(e.toolCallId());
                    toolCallArgsBuffer.remove(e.toolCallId());
                }
                case AguiEvent.ToolCallResult e -> listener.onToolCallResult(e, pageWrapper);
                case AguiEvent.StateSnapshot e -> listener.onStateSnapshot(e, pageWrapper);
                case AguiEvent.StateDelta e -> listener.onStateDelta(e, pageWrapper);
                case AguiEvent.Custom e -> {
                    String name = e.name();
                    if ("HUMAN_APPROVAL".equals(name)) listener.onHumanApproval(e, pageWrapper);
                    else if ("TOOL_EXECUTE".equals(name)) listener.onToolExecute(e, pageWrapper);
                    else if ("AGENT_OUTPUT".equals(name) || "AGENT_OUTPUT_END".equals(name)) listener.onAgentOutput(e, pageWrapper);
                    else listener.onCustomEvent(e, pageWrapper);
                }
                default -> { }
            }
        } finally {
            // RunFinished 的 listener 回调全部执行完毕后，才通知 collector 完成
            // 这确保 awaitCompletion 返回时 onRunFinished 已经执行完，session.close() 安全
            if (event instanceof AguiEvent.RunFinished && collector != null) {
                collector.signalCompletion();
            }
        }
    }

    /**
     * 解析 AskUserQuestion 参数并通知监听器
     */
    @SuppressWarnings("unchecked")
    private void notifyAskUserQuestion(String toolCallId) {
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
        if (listener != null) listener.onAskUserQuestion(toolCallId, questions, pageWrapper);
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

        /**
         * 添加事件到列表（不触发 completionLatch）
         * <p>
         * completionLatch 由 {@link HeadlessBrowserSession#handleSseEvent} 在
         * listener 回调全部执行完毕后显式调用 {@link #signalCompletion} 触发，
         * 确保 awaitCompletion 返回时回调已全部完成，session.close() 安全。
         */
        void addEvent(AguiEvent event) {
            events.add(event);
        }

        /** RunFinished 回调执行完毕后，由 handleSseEvent 调用 */
        void signalCompletion() {
            completionLatch.countDown();
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
