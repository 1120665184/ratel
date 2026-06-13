package org.quyq.gwsu.security.headless;

import com.google.gson.Gson;
import com.microsoft.playwright.*;
import io.agentscope.core.agui.event.AguiEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 无头浏览器用户会话
 *
 * 核心职责：
 * 1. 管理用户的 BrowserContext 和 Page
 * 2. 通过 page.exposeFunction + fetch 拦截实现 SSE 事件实时回调
 * 3. 操作界面触发助手并处理审批/提问
 */
@Slf4j
public class HeadlessBrowserSession implements AutoCloseable {

    private static final String SSE_CALLBACK_NAME = "__onHeadlessSseEvent";
    private static final String FETCH_INTERCEPTOR_JS_RESOURCE = "headless/sse-fetch-interceptor.js";

    private final String userId;
    private final BrowserContext context;
    private final Page page;
    private final HeadlessSseEventParser parser = new HeadlessSseEventParser();
    private final Gson gson = new Gson();
    private volatile long lastAccessTime = System.currentTimeMillis();

    /** 当前活跃的 SSE 事件收集器（每次 sendMessage 重建） */
    private final AtomicReference<SseEventCollector> currentCollector = new AtomicReference<>();

    /** 当前活跃的监听器和处理器（每次 sendMessage 更新） */
    private final AtomicReference<HeadlessAgentListener> currentListener = new AtomicReference<>();
    private final AtomicReference<HeadlessApprovalHandler> currentApprovalHandler = new AtomicReference<>();
    private final AtomicReference<SseEventCollector> currentEventCollector = new AtomicReference<>();

    /** toolCallId → toolCallName 映射（从 TOOL_CALL_START 事件维护） */
    private final ConcurrentHashMap<String, String> toolCallNameMap = new ConcurrentHashMap<>();

    /** toolCallId → 累积的 TOOL_CALL_ARGS delta（用于 AskUserQuestion 参数解析） */
    private final ConcurrentHashMap<String, StringBuilder> toolCallArgsBuffer = new ConcurrentHashMap<>();

    /** 等待审批的 CompletableFuture */
    private volatile CompletableFuture<HeadlessApprovalHandler.ApprovalResult> approvalFuture;
    /** 等待提问回答的 CompletableFuture */
    private volatile CompletableFuture<Map<String, String>> askQuestionFuture;

    /** fetch 拦截器是否已注入 */
    private volatile boolean fetchInterceptorInjected = false;

    HeadlessBrowserSession(String userId, BrowserContext context) {
        this.userId = userId;
        this.context = context;
        this.page = context.newPage();
    }

    /**
     * 发送消息给助手并实时监听 SSE 事件
     *
     * @param message 用户消息
     * @param listener 事件监听器（实时回调）
     * @param approvalHandler 审批处理器（可为 null）
     * @return 所有 SSE 事件的汇总列表
     */
    public List<AguiEvent> sendMessage(
            String message,
            HeadlessAgentListener listener,
            HeadlessApprovalHandler approvalHandler) {

        lastAccessTime = System.currentTimeMillis();

        // 清理上一次调用的状态
        toolCallNameMap.clear();
        toolCallArgsBuffer.clear();

        // 1. 确保 fetch 拦截器已注入
        ensureFetchInterceptor();

        // 2. 创建事件收集器
        SseEventCollector collector = new SseEventCollector();
        currentCollector.set(collector);
        currentEventCollector.set(collector);

        // 3. 设置当前监听器和处理器
        currentListener.set(listener);
        currentApprovalHandler.set(approvalHandler);

        // 4. 操作界面：填入消息 + 点击发送
        triggerAssistant(message);

        // 5. 阻塞等待 RUN_FINISHED 或超时
        collector.awaitCompletion();

        return collector.getEvents();
    }

    /**
     * 便捷方法：不带审批处理器
     */
    public List<AguiEvent> sendMessage(String message, HeadlessAgentListener listener) {
        return sendMessage(message, listener, null);
    }

    /**
     * 外部主动批准（当没有 approvalHandler 时使用）
     */
    public void approve() {
        approve(null);
    }

    /**
     * 外部主动批准（带审批结果）
     */
    public void approve(HeadlessApprovalHandler.ApprovalResult result) {
        if (approvalFuture != null) {
            approvalFuture.complete(result != null ? result : HeadlessApprovalHandler.ApprovalResult.approved());
        }
    }

    /**
     * 外部主动拒绝
     */
    public void reject(String reason) {
        if (approvalFuture != null) {
            approvalFuture.complete(HeadlessApprovalHandler.ApprovalResult.rejected(reason));
        }
    }

    /**
     * 外部主动回答提问
     */
    public void answerQuestion(Map<String, String> answers) {
        if (askQuestionFuture != null) {
            askQuestionFuture.complete(answers);
        }
    }

    /**
     * 确保页面导航到首页
     */
    public void ensureHomePage(String baseUrl) {
        String currentUrl = page.url();
        if (currentUrl == null || currentUrl.isEmpty() || currentUrl.equals("about:blank")) {
            page.navigate(baseUrl);
            page.waitForLoadState();
            fetchInterceptorInjected = false;
        }
    }

    public boolean isIdleTimeout(long timeoutMs) {
        return System.currentTimeMillis() - lastAccessTime > timeoutMs;
    }

    @Override
    public void close() {
        toolCallNameMap.clear();
        toolCallArgsBuffer.clear();
        try {
            if (page != null && !page.isClosed()) page.close();
        } catch (Exception e) {
            log.warn("关闭 Page 异常: userId={}", userId, e);
        }
        try {
            if (context != null) context.close();
        } catch (Exception e) {
            log.warn("关闭 BrowserContext 异常: userId={}", userId, e);
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 注入 fetch 拦截器
     */
    private void ensureFetchInterceptor() {
        if (fetchInterceptorInjected) return;

        try {
            // 注册 exposeFunction：JS 调用 __onHeadlessSseEvent 时触发 Java 回调
            page.exposeFunction(SSE_CALLBACK_NAME, (arg) -> {
                handleSseEvent(String.valueOf(arg));
                return null;
            });

            // 读取并注入 JS 脚本
            String jsScript = loadJsScript();
            page.evaluate(jsScript);

            fetchInterceptorInjected = true;
            log.debug("Fetch 拦截器注入成功: userId={}", userId);
        } catch (PlaywrightException e) {
            if (e.getMessage() != null && e.getMessage().contains("already been registered")) {
                log.debug("exposeFunction 已注册，重新注入 JS: userId={}", userId);
                try {
                    String jsScript = loadJsScript();
                    page.evaluate(jsScript);
                    fetchInterceptorInjected = true;
                } catch (Exception ex) {
                    log.error("重新注入 JS 失败: userId={}", userId, ex);
                }
            } else {
                log.error("Fetch 拦截器注入失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 完整的 SSE 事件处理（从 exposeFunction 回调触发）
     */
    private void handleSseEvent(String eventJson) {
        AguiEvent event = parser.parseEvent(eventJson);
        if (event == null) return;

        SseEventCollector collector = currentEventCollector.get();
        HeadlessAgentListener listener = currentListener.get();

        if (collector != null) collector.addEvent(event);
        if (listener == null) return;

        // 总入口回调
        listener.onEvent(event);

        // 按类型分发
        switch (event) {
            case AguiEvent.RunStarted e -> listener.onRunStarted(e);
            case AguiEvent.RunFinished e -> {
                listener.onRunFinished(e);
            }
            case AguiEvent.TextMessageStart e -> listener.onTextMessageStart(e);
            case AguiEvent.TextMessageContent e -> listener.onTextMessageContent(e.delta());
            case AguiEvent.TextMessageEnd e -> listener.onTextMessageEnd(e);
            case AguiEvent.ToolCallStart e -> {
                listener.onToolCallStart(e);
                toolCallNameMap.put(e.toolCallId(), e.toolCallName());
                if ("AskUserQuestion".equals(e.toolCallName())) {
                    toolCallArgsBuffer.put(e.toolCallId(), new StringBuilder());
                }
            }
            case AguiEvent.ToolCallArgs e -> {
                listener.onToolCallArgs(e);
                StringBuilder argsBuf = toolCallArgsBuffer.get(e.toolCallId());
                if (argsBuf != null) {
                    argsBuf.append(e.delta());
                }
            }
            case AguiEvent.ToolCallEnd e -> {
                listener.onToolCallEnd(e);
                String toolCallName = toolCallNameMap.get(e.toolCallId());
                if ("AskUserQuestion".equals(toolCallName)) {
                    handleAskUserQuestionEvent(e.toolCallId());
                }
                toolCallNameMap.remove(e.toolCallId());
                toolCallArgsBuffer.remove(e.toolCallId());
            }
            case AguiEvent.ToolCallResult e -> listener.onToolCallResult(e);
            case AguiEvent.Custom e -> {
                String name = e.name();
                if ("HUMAN_APPROVAL".equals(name)) {
                    listener.onHumanApproval(e);
                    handleHumanApprovalEvent(e);
                } else if ("TOOL_EXECUTE".equals(name)) {
                    listener.onToolExecute(e);
                } else if ("AGENT_OUTPUT".equals(name) || "AGENT_OUTPUT_END".equals(name)) {
                    listener.onAgentOutput(e);
                } else {
                    listener.onCustomEvent(e);
                }
            }
            case AguiEvent.StateSnapshot e -> listener.onStateSnapshot(e);
            case AguiEvent.StateDelta e -> listener.onStateDelta(e);
            default -> { }
        }
    }

    /**
     * 处理 HUMAN_APPROVAL 事件
     */
    private void handleHumanApprovalEvent(AguiEvent.Custom event) {
        HeadlessApprovalHandler handler = currentApprovalHandler.get();
        if (handler != null) {
            HeadlessApprovalHandler.ApprovalResult result = handler.handleApproval(event);
            operateApprovalUI(result);
        } else {
            approvalFuture = new CompletableFuture<>();
            try {
                HeadlessApprovalHandler.ApprovalResult result = approvalFuture.get(5, TimeUnit.MINUTES);
                operateApprovalUI(result);
            } catch (TimeoutException e) {
                log.warn("审批等待超时: userId={}", userId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("审批处理异常: userId={}", userId, e);
            }
        }
    }

    /**
     * 处理 AskUserQuestion 事件
     */
    @SuppressWarnings("unchecked")
    private void handleAskUserQuestionEvent(String toolCallId) {
        StringBuilder argsBuf = toolCallArgsBuffer.get(toolCallId);
        Map<String, Object> questions = Map.of();
        if (argsBuf != null && !argsBuf.isEmpty()) {
            try {
                Map<String, Object> parsed = gson.fromJson(argsBuf.toString(), Map.class);
                if (parsed != null) questions = parsed;
            } catch (Exception e) {
                log.warn("AskUserQuestion 参数解析失败: toolCallId={}", toolCallId, e);
            }
        }

        HeadlessAgentListener listener = currentListener.get();
        if (listener != null) {
            listener.onAskUserQuestion(toolCallId, questions);
        }

        HeadlessApprovalHandler handler = currentApprovalHandler.get();
        if (handler != null) {
            Map<String, String> answers = handler.handleAskUserQuestion(toolCallId, questions);
            operateAskUserQuestionUI(answers);
        } else {
            askQuestionFuture = new CompletableFuture<>();
            try {
                Map<String, String> answers = askQuestionFuture.get(5, TimeUnit.MINUTES);
                operateAskUserQuestionUI(answers);
            } catch (TimeoutException e) {
                log.warn("提问等待超时: userId={}, toolCallId={}", userId, toolCallId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("提问处理异常: userId={}", userId, e);
            }
        }
    }

    /**
     * 操作界面审批按钮
     */
    private void operateApprovalUI(HeadlessApprovalHandler.ApprovalResult result) {
        try {
            if (result.approved()) {
                page.locator("button:has-text('批准')").click();
            } else {
                page.locator("button:has-text('拒绝')").click();
                if (result.rejectReason() != null && !result.rejectReason().isEmpty()) {
                    page.locator("textarea").last().fill(result.rejectReason());
                    page.locator("button:has-text('提交')").last().click();
                }
            }
        } catch (Exception e) {
            log.error("审批界面操作失败: userId={}", userId, e);
        }
    }

    /**
     * 操作界面 AskUserQuestion 选择并提交
     */
    private void operateAskUserQuestionUI(Map<String, String> answers) {
        try {
            for (Map.Entry<String, String> entry : answers.entrySet()) {
                page.locator(String.format(".optionItem:has-text('%s')", entry.getValue())).click();
            }
            page.locator("button:has-text('提交答案')").click();
        } catch (Exception e) {
            log.error("提问界面操作失败: userId={}", userId, e);
        }
    }

    /**
     * 操作界面触发助手
     */
    private void triggerAssistant(String message) {
        try {
            Locator textarea = page.locator("textarea").first();
            textarea.fill(message);
            page.locator("button[type='submit']").click();
            log.debug("助手消息已发送: userId={}, message={}", userId, message);
        } catch (Exception e) {
            log.error("触发助手失败: userId={}", userId, e);
            throw new RuntimeException("触发助手失败", e);
        }
    }

    /**
     * 读取 JS 脚本资源文件
     */
    private String loadJsScript() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(FETCH_INTERCEPTOR_JS_RESOURCE)) {
            if (is == null) {
                throw new IOException("找不到资源文件: " + FETCH_INTERCEPTOR_JS_RESOURCE);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载 JS 脚本失败", e);
        }
    }

    // ==================== 事件收集器 ====================

    static class SseEventCollector {
        private final List<AguiEvent> events = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch completionLatch = new CountDownLatch(1);
        private volatile Throwable error;

        void addEvent(AguiEvent event) {
            events.add(event);
            if (event instanceof AguiEvent.RunFinished) {
                completionLatch.countDown();
            }
        }

        void signalError(Throwable t) {
            this.error = t;
            completionLatch.countDown();
        }

        List<AguiEvent> awaitCompletion() {
            try {
                completionLatch.await(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getEvents();
        }

        List<AguiEvent> getEvents() {
            return List.copyOf(events);
        }

        Throwable getError() {
            return error;
        }
    }
}
