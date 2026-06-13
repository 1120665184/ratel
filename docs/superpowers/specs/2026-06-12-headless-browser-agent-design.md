# Playwright 无头浏览器智能体 - 设计文档

> 日期：2026-06-12
> 模块：business-security-server
> 状态：设计评审中

## 1. 概述

通过 Playwright 无头浏览器操作项目界面，与页面上的 CopilotKit 智能助手交互。提供 Java 工具类接口，支持：

- 自动登录并导航到首页
- 触发助手发送消息
- **实时流式监听** SSE 事件（通过 `page.exposeFunction` + fetch 拦截注入）
- 处理 HUMAN_APPROVAL 审批和 AskUserQuestion 交互
- 同一用户复用 BrowserContext，闲置超时自动回收
- 并发数可配置控制

**不包含**：快速登录服务（HeadlessLoginService），后续单独设计。

## 2. 模块结构

```
business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/headless/
├── HeadlessBrowserManager.java            # 核心管理器入口
├── HeadlessBrowserSession.java            # 用户会话
├── HeadlessSseEventParser.java            # SSE 事件解析器
├── HeadlessAgentListener.java             # 监听器接口
├── HeadlessApprovalHandler.java           # 审批/提问处理器接口
├── SseFetchInterceptor.js                 # fetch 拦截器 JS 脚本（资源文件）
└── config/
    └── HeadlessBrowserConfiguration.java  # Spring 配置类
```

**说明**：事件模型直接复用 `io.agentscope.core.agui.event.AguiEvent` 及其子类型，不再自定义事件 DTO。

## 3. 核心类设计

### 3.1 事件模型（复用 AguiEvent）

SSE 事件解析后直接映射为 `io.agentscope.core.agui.event.AguiEvent` 的各子类型：

| AguiEventType | AguiEvent 子类型 | 关键字段 |
|---|---|---|
| RUN_STARTED | `AguiEvent.RunStarted` | threadId, runId |
| RUN_FINISHED | `AguiEvent.RunFinished` | threadId, runId |
| TEXT_MESSAGE_START | `AguiEvent.TextMessageStart` | threadId, runId, messageId, role |
| TEXT_MESSAGE_CONTENT | `AguiEvent.TextMessageContent` | threadId, runId, messageId, delta |
| TEXT_MESSAGE_END | `AguiEvent.TextMessageEnd` | threadId, runId, messageId |
| TOOL_CALL_START | `AguiEvent.ToolCallStart` | threadId, runId, toolCallId, toolCallName |
| TOOL_CALL_ARGS | `AguiEvent.ToolCallArgs` | threadId, runId, toolCallId, delta |
| TOOL_CALL_END | `AguiEvent.ToolCallEnd` | threadId, runId, toolCallId |
| TOOL_CALL_RESULT | `AguiEvent.ToolCallResult` | threadId, runId, toolCallId, content |
| CUSTOM | `AguiEvent.Custom` | threadId, runId, name, value |
| STATE_SNAPSHOT | `AguiEvent.StateSnapshot` | threadId, runId, snapshot |
| STATE_DELTA | `AguiEvent.StateDelta` | threadId, runId, delta |
| RAW | `AguiEvent.Raw` | threadId, runId, rawEvent |

**自定义事件名称**（CUSTOM 类型的 name 字段）：

| name | 含义 |
|---|---|
| `HUMAN_APPROVAL` | 人工审批请求 |
| `TOOL_EXECUTE` | 前端工具执行请求 |
| `AGENT_OUTPUT` | AI 输出视图 |
| `AGENT_OUTPUT_END` | AI 输出结束 |

**AskUserQuestion** 不是 CUSTOM 事件，而是通过 `TOOL_CALL_END` 中 `toolCallName=AskUserQuestion` 识别（前端 CopilotKit 在 `onToolCallEndEvent` 中判断）。

### 3.2 HeadlessAgentListener

监听器接口，业务端实现此接口处理不同类型的 SSE 事件。所有方法默认为空实现。回调参数直接使用 `AguiEvent` 各子类型。

```java
package org.quyq.gwsu.security.headless;

import io.agentscope.core.agui.event.AguiEvent;

import java.util.Map;

/**
 * 无头浏览器智能体事件监听器
 * 所有回调在 SSE 事件到达时实时触发，无需等待流结束
 */
public interface HeadlessAgentListener {

    // ==================== 通用回调 ====================

    /**
     * 收到任意 SSE 事件（总入口，先于具体回调执行）
     * 可用于日志记录、事件统计等
     */
    default void onEvent(AguiEvent event) {}

    // ==================== 生命周期事件 ====================

    /** RUN_STARTED：智能体开始运行 */
    default void onRunStarted(AguiEvent.RunStarted event) {}

    /** RUN_FINISHED：智能体运行结束 */
    default void onRunFinished(AguiEvent.RunFinished event) {}

    // ==================== 文本消息事件 ====================

    /** TEXT_MESSAGE_START：文本消息开始 */
    default void onTextMessageStart(AguiEvent.TextMessageStart event) {}

    /**
     * TEXT_MESSAGE_CONTENT：文本消息增量内容
     * @param delta 本次增量文本片段
     */
    default void onTextMessageContent(String delta) {}

    /** TEXT_MESSAGE_END：文本消息结束 */
    default void onTextMessageEnd(AguiEvent.TextMessageEnd event) {}

    // ==================== 工具调用事件 ====================

    /** TOOL_CALL_START：工具调用开始 */
    default void onToolCallStart(AguiEvent.ToolCallStart event) {}

    /** TOOL_CALL_ARGS：工具调用参数增量 */
    default void onToolCallArgs(AguiEvent.ToolCallArgs event) {}

    /** TOOL_CALL_END：工具调用结束 */
    default void onToolCallEnd(AguiEvent.ToolCallEnd event) {}

    /** TOOL_CALL_RESULT：工具调用结果 */
    default void onToolCallResult(AguiEvent.ToolCallResult event) {}

    // ==================== 自定义事件 ====================

    /**
     * HUMAN_APPROVAL：人工审批请求（CUSTOM 类型，name=HUMAN_APPROVAL）
     * 如果注册了 HeadlessApprovalHandler，Session 会自动处理
     * 否则需手动调用 Session.approve() / Session.reject()
     */
    default void onHumanApproval(AguiEvent.Custom event) {}

    /**
     * AskUserQuestion：智能体提问
     * 通过 TOOL_CALL_END 中 toolCallName=AskUserQuestion 识别
     * 前端在 onToolCallEndEvent 回调中分发，此时弹出选择框
     * 如果注册了 HeadlessApprovalHandler，Session 会自动处理
     * 否则需手动调用 Session.answerQuestion()
     */
    default void onAskUserQuestion(AguiEvent.ToolCallEnd event) {}

    /** TOOL_EXECUTE：前端工具执行请求（CUSTOM 类型） */
    default void onToolExecute(AguiEvent.Custom event) {}

    /** AGENT_OUTPUT：AI 输出视图（CUSTOM 类型） */
    default void onAgentOutput(AguiEvent.Custom event) {}

    /** 其他未识别的自定义事件 */
    default void onCustomEvent(AguiEvent.Custom event) {}

    // ==================== 状态事件 ====================

    /** STATE_SNAPSHOT：完整状态快照 */
    default void onStateSnapshot(AguiEvent.StateSnapshot event) {}

    /** STATE_DELTA：增量状态变更 */
    default void onStateDelta(AguiEvent.StateDelta event) {}

    // ==================== 错误处理 ====================

    /** SSE 流发生错误 */
    default void onError(Throwable error) {}
}
```

### 3.3 HeadlessApprovalHandler

审批与提问处理器，用于自动处理 HUMAN_APPROVAL 和 AskUserQuestion 事件。

```java
package org.quyq.gwsu.security.headless;

import io.agentscope.core.agui.event.AguiEvent;

import java.util.Map;

/**
 * 无头浏览器审批/提问处理器
 * 注册后，Session 遇到 HUMAN_APPROVAL 或 AskUserQuestion 时自动调用
 */
public interface HeadlessApprovalHandler {

    /**
     * 处理 HUMAN_APPROVAL 事件
     * Session 会根据返回结果自动操作界面上的审批按钮
     *
     * @param event 审批事件（CUSTOM 类型），event.value() 包含审批详情
     * @return 审批结果
     */
    ApprovalResult handleApproval(AguiEvent.Custom event);

    /**
     * 处理 AskUserQuestion 事件
     * Session 会根据返回的答案自动操作界面上的选择框并提交
     *
     * @param event 提问事件（TOOL_CALL_END 类型）
     * @return 问题答案，key 为问题文本，value 为用户选择的答案
     */
    Map<String, String> handleAskUserQuestion(AguiEvent.ToolCallEnd event);

    /**
     * 审批结果
     */
    record ApprovalResult(boolean approved, String rejectReason) {
        public static ApprovalResult accept() {
            return new ApprovalResult(true, null);
        }

        public static ApprovalResult reject(String reason) {
            return new ApprovalResult(false, reason);
        }
    }
}
```

### 3.4 HeadlessSseEventParser

SSE 事件解析器，将原始 SSE JSON 解析为 `AguiEvent` 各子类型。解析逻辑与 `@ag-ui/client` 的 `parseSSEStream` 一致。

```java
package org.quyq.gwsu.security.headless;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.event.AguiEventType;
import com.microsoft.playwright.Response;

import java.util.*;

/**
 * SSE 事件解析器
 *
 * 将 SSE 原始 JSON 解析为 AguiEvent 各子类型，
 * 与后端 AguiController 发送事件的序列化格式对称。
 *
 * 解析流程：
 * 1. JS 拦截器消费 ReadableStream，逐 chunk 解析 SSE 文本
 * 2. 每解析到一个完整 JSON，通过 exposeFunction 传入 Java
 * 3. 本解析器将 JSON 反序列化为对应的 AguiEvent 子类型
 */
public class HeadlessSseEventParser {

    private final Gson gson = new Gson();

    /**
     * 解析单个 SSE 事件 JSON 为 AguiEvent 子类型
     *
     * 根据 JSON 中的 type 字段路由到对应的 AguiEvent record 构造
     */
    public AguiEvent parseEvent(String eventJson) {
        Map<String, Object> raw = gson.fromJson(eventJson, new TypeToken<Map<String, Object>>() {});
        if (raw == null) return null;

        String typeStr = (String) raw.getOrDefault("type", "");
        AguiEventType type;
        try {
            type = AguiEventType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            // 未知类型，返回 Raw
            return new AguiEvent.Raw(
                (String) raw.getOrDefault("threadId", ""),
                (String) raw.getOrDefault("runId", ""),
                raw
            );
        }

        String threadId = (String) raw.getOrDefault("threadId", "");
        String runId = (String) raw.getOrDefault("runId", "");

        return switch (type) {
            case RUN_STARTED -> new AguiEvent.RunStarted(threadId, runId);
            case RUN_FINISHED -> new AguiEvent.RunFinished(threadId, runId);
            case TEXT_MESSAGE_START -> new AguiEvent.TextMessageStart(
                threadId, runId,
                (String) raw.getOrDefault("messageId", ""),
                (String) raw.getOrDefault("role", "assistant"));
            case TEXT_MESSAGE_CONTENT -> new AguiEvent.TextMessageContent(
                threadId, runId,
                (String) raw.getOrDefault("messageId", ""),
                (String) raw.getOrDefault("delta", ""));
            case TEXT_MESSAGE_END -> new AguiEvent.TextMessageEnd(
                threadId, runId,
                (String) raw.getOrDefault("messageId", ""));
            case TOOL_CALL_START -> new AguiEvent.ToolCallStart(
                threadId, runId,
                (String) raw.getOrDefault("toolCallId", ""),
                (String) raw.getOrDefault("toolCallName", ""));
            case TOOL_CALL_ARGS -> new AguiEvent.ToolCallArgs(
                threadId, runId,
                (String) raw.getOrDefault("toolCallId", ""),
                (String) raw.getOrDefault("delta", ""));
            case TOOL_CALL_END -> new AguiEvent.ToolCallEnd(
                threadId, runId,
                (String) raw.getOrDefault("toolCallId", ""));
            case TOOL_CALL_RESULT -> new AguiEvent.ToolCallResult(
                threadId, runId,
                (String) raw.getOrDefault("toolCallId", ""),
                raw.getOrDefault("content", null));
            case CUSTOM -> new AguiEvent.Custom(
                threadId, runId,
                (String) raw.getOrDefault("name", ""),
                raw.getOrDefault("value", null));
            case STATE_SNAPSHOT -> new AguiEvent.StateSnapshot(
                threadId, runId,
                raw.getOrDefault("snapshot", null));
            case STATE_DELTA -> new AguiEvent.StateDelta(
                threadId, runId,
                raw.getOrDefault("delta", null));
            case RAW -> new AguiEvent.Raw(
                threadId, runId,
                raw.getOrDefault("rawEvent", null));
            // REASONING 相关事件
            default -> new AguiEvent.Raw(threadId, runId, raw);
        };
    }

    /**
     * 判断 HTTP 响应是否为 SSE 流
     */
    public static boolean isSseResponse(Response response) {
        String contentType = response.headers().get("content-type");
        return contentType != null && contentType.contains("text/event-stream");
    }
}
```

### 3.5 HeadlessBrowserSession

用户会话，封装 BrowserContext + Page + SSE 事件拦截。每个用户对应一个 Session。

```java
package org.quyq.gwsu.security.headless;

import com.microsoft.playwright.*;
import io.agentscope.core.agui.event.AguiEvent;
import lombok.extern.slf4j.Slf4j;

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

    private final String userId;
    private final BrowserContext context;
    private Page page;
    private final HeadlessSseEventParser parser = new HeadlessSseEventParser();
    private volatile long lastAccessTime;

    /** 当前活跃的 SSE 事件收集器（每次 sendMessage 重建） */
    private final AtomicReference<SseEventCollector> currentCollector = new AtomicReference<>();

    /** 等待审批的 CompletableFuture */
    private volatile CompletableFuture<HeadlessApprovalHandler.ApprovalResult> approvalFuture;
    /** 等待提问回答的 CompletableFuture */
    private volatile CompletableFuture<Map<String, String>> askQuestionFuture;

    HeadlessBrowserSession(String userId, BrowserContext context) { ... }

    /**
     * 发送消息给助手并实时监听 SSE 事件
     *
     * 流程：
     * 1. 确保页面已就绪（首页 + 助手可用）
     * 2. 注册 exposeFunction + fetch 拦截器
     * 3. 通过 Playwright 填入消息并点击发送
     * 4. 实时回调 listener 的各方法
     * 5. 遇到 HUMAN_APPROVAL/AskUserQuestion 时：
     *    - 如果有 approvalHandler → 自动处理
     *    - 如果没有 → 暂停等待外部调用 approve/reject/answerQuestion
     * 6. RUN_FINISHED 后返回全部事件汇总
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

        // 1. 确保 fetch 拦截器已注入
        ensureFetchInterceptor();

        // 2. 创建事件收集器
        SseEventCollector collector = new SseEventCollector();
        currentCollector.set(collector);

        // 3. 注册 exposeFunction 回调
        registerSseCallback(collector, listener, approvalHandler);

        // 4. 操作界面：填入消息 + 点击发送
        triggerAssistant(message);

        // 5. 阻塞等待 RUN_FINISHED 或超时
        collector.awaitCompletion();

        return collector.getEvents();
    }

    /**
     * 便捷方法：不带审批处理器
     */
    public List<AguiEvent> sendMessage(
            String message,
            HeadlessAgentListener listener) {
        return sendMessage(message, listener, null);
    }

    /**
     * 外部主动批准（当没有 approvalHandler 时使用）
     */
    public void approve() { ... }

    /**
     * 外部主动拒绝
     */
    public void reject(String reason) { ... }

    /**
     * 外部主动回答提问
     */
    public void answerQuestion(Map<String, String> answers) { ... }

    /**
     * 确保页面导航到首页
     */
    public void ensureHomePage(String baseUrl) { ... }

    public boolean isIdleTimeout(long timeoutMs) {
        return System.currentTimeMillis() - lastAccessTime > timeoutMs;
    }

    @Override
    public void close() {
        if (page != null) page.close();
        context.close();
    }

    // ==================== 内部实现 ====================

    /**
     * 注入 fetch 拦截器
     *
     * 核心原理：
     * 1. monkey-patch window.fetch
     * 2. 当检测到 /brain/run/copilotKit 的 POST SSE 响应时
     * 3. 克隆 response 的 ReadableStream（tee）
     * 4. 逐 chunk 解析 SSE 事件（与 @ag-ui/client parseSSEStream 逻辑一致）
     * 5. 每解析到一个完整事件，调用 window.__onHeadlessSseEvent(eventJson)
     * 6. __onHeadlessSseEvent 通过 page.exposeFunction 映射到 Java 回调
     */
    private void ensureFetchInterceptor() { ... }

    /**
     * 注册 exposeFunction：JS → Java 实时回调
     *
     * 回调内根据 AguiEvent 类型路由到 listener 的具体方法：
     * - RUN_STARTED → listener.onRunStarted()
     * - TEXT_MESSAGE_CONTENT → listener.onTextMessageContent(delta)
     * - CUSTOM(name=HUMAN_APPROVAL) → listener.onHumanApproval() + approvalHandler
     * - TOOL_CALL_END(toolCallName=AskUserQuestion) → listener.onAskUserQuestion() + approvalHandler
     * - RUN_FINISHED → listener.onRunFinished() + 释放 CountDownLatch
     * - 其他类型 → 对应的 listener 方法
     */
    private void registerSseCallback(
            SseEventCollector collector,
            HeadlessAgentListener listener,
            HeadlessApprovalHandler approvalHandler) { ... }

    /**
     * 操作界面触发助手
     */
    private void triggerAssistant(String message) {
        // 定位 CopilotChat 输入框，填入消息
        // 点击发送按钮
        // 等待助手响应（SSE 连接建立）
    }

    /**
     * 处理 HUMAN_APPROVAL 事件
     * 如果有 approvalHandler → 调用处理 → 操作界面审批按钮
     * 如果没有 → 创建 CompletableFuture 等待外部调用
     */
    private void handleHumanApproval(
            AguiEvent.Custom event,
            HeadlessApprovalHandler approvalHandler) { ... }

    /**
     * 处理 AskUserQuestion 事件
     * 如果有 approvalHandler → 调用处理 → 操作界面选择并提交
     * 如果没有 → 创建 CompletableFuture 等待外部调用
     */
    private void handleAskUserQuestion(
            AguiEvent.ToolCallEnd event,
            HeadlessApprovalHandler approvalHandler) { ... }

    // ==================== 事件收集器 ====================

    /**
     * SSE 事件收集器
     * 收集一次 sendMessage 调用中的所有事件
     * 通过 CountDownLatch 等待 RUN_FINISHED
     */
    private static class SseEventCollector {
        private final List<AguiEvent> events = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch completionLatch = new CountDownLatch(1);

        void addEvent(AguiEvent event) {
            events.add(event);
            if (event instanceof AguiEvent.RunFinished) {
                completionLatch.countDown();
            }
        }

        void signalError() {
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
    }
}
```

### 3.6 HeadlessBrowserManager

核心管理器，提供统一的对外接口。管理 Browser 生命周期、Session 池、并发控制、闲置回收。

```java
package org.quyq.gwsu.security.headless;

import com.microsoft.playwright.*;
import io.agentscope.core.agui.event.AguiEvent;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.security.headless.config.HeadlessBrowserConfiguration;

import java.util.List;
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

        // 启动 Playwright 和 Browser（全局共享）
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(config.isHeadless()));

        // 启动闲置检查定时器
        this.idleChecker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "headless-idle-checker");
            t.setDaemon(true);
            return t;
        });
        this.idleChecker.scheduleAtFixedRate(
                this::checkIdleSessions, 1, 1, TimeUnit.MINUTES);
    }

    // ==================== 对外接口 ====================

    /**
     * 发送消息给助手（无审批处理器）
     */
    public List<AguiEvent> sendMessage(
            String userId,
            String message,
            HeadlessAgentListener listener) {
        return sendMessage(userId, message, listener, null);
    }

    /**
     * 发送消息给助手（带审批处理器）
     *
     * 1. 获取/创建该用户的 Session（受并发控制）
     * 2. 委托 Session.sendMessage 执行
     */
    public List<AguiEvent> sendMessage(
            String userId,
            String message,
            HeadlessAgentListener listener,
            HeadlessApprovalHandler approvalHandler) {

        HeadlessBrowserSession session = getOrCreateSession(userId);
        return session.sendMessage(message, listener, approvalHandler);
    }

    /**
     * 获取用户的 Session，不存在则创建
     *
     * 并发控制：
     * - acquire Semaphore 许可，无可用许可时阻塞等待
     * - 创建新 Session 后放入 sessions 池
     * - 如果用户已有 Session，释放刚获取的许可
     */
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
            // double-check：computeIfAbsent 内创建，如果已存在则释放多获取的许可
            final boolean[] created = {false};
            HeadlessBrowserSession session = sessions.computeIfAbsent(userId, id -> {
                created[0] = true;
                log.info("创建无头浏览器 Session: userId={}", id);
                BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
                        .setViewportSize(1920, 1080));
                return new HeadlessBrowserSession(id, ctx);
            });
            if (!created[0]) {
                // 其他线程抢先创建了，释放多余的许可
                concurrencyLimiter.release();
            }
            return session;
        } catch (Exception e) {
            concurrencyLimiter.release();
            throw e;
        }
    }

    /**
     * 关闭指定用户的 Session
     */
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

    /** 当前活跃 Session 数 */
    public int activeSessionCount() {
        return sessions.size();
    }

    @Override
    public void close() {
        idleChecker.shutdown();
        sessions.forEach((userId, session) -> {
            try { session.close(); } catch (Exception e) { log.warn("关闭异常", e); }
        });
        sessions.clear();
        browser.close();
        playwright.close();
    }

    // ==================== 内部方法 ====================

    /**
     * 闲置超时检查（每分钟执行一次）
     * 超过 idleTimeoutMs 未访问的 Session 自动关闭释放资源
     */
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
```

### 3.7 HeadlessBrowserConfiguration

```java
package org.quyq.gwsu.security.headless.config;

import lombok.Data;
import org.quyq.gwsu.security.headless.HeadlessBrowserManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "headless.browser")
public class HeadlessBrowserConfiguration {

    /** 最大并发 BrowserContext 数量 */
    private int maxContexts = 30;

    /** 闲置超时时间（毫秒），默认 10 分钟 */
    private long idleTimeoutMs = 600_000L;

    /** SSE 等待超时时间（毫秒），默认 5 分钟 */
    private long sseTimeoutMs = 300_000L;

    /** 是否无头模式 */
    private boolean headless = true;

    /** 应用基础 URL */
    private String baseUrl = "http://localhost:8000";

    @Bean(destroyMethod = "close")
    public HeadlessBrowserManager headlessBrowserManager() {
        return new HeadlessBrowserManager(this);
    }
}
```

## 4. SSE 实时拦截方案

### 4.1 核心原理

```
┌─────────────────────────────────────────────────────────┐
│                    Playwright Page                       │
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │         注入的 fetch 拦截器 (JS)                    │  │
│  │                                                   │  │
│  │  window.fetch = (original) => (url, init) => {    │  │
│  │    const response = original(url, init);          │  │
│  │    if (isSSE(response)) {                         │  │
│  │      const [stream1, stream2] = response.body     │  │
│  │        .tee();                                    │  │
│  │      // stream1 → 前端正常消费                     │  │
│  │      // stream2 → 拦截器解析 SSE 事件              │  │
│  │      consumeStream(stream2, event => {            │  │
│  │        window.__onHeadlessSseEvent(event);         │  │
│  │      });                                          │  │
│  │      return new Response(stream1, ...);           │  │
│  │    }                                              │  │
│  │    return response;                               │  │
│  │  }                                                │  │
│  └───────────────────┬───────────────────────────────┘  │
│                      │                                  │
│                      ▼                                  │
│          window.__onHeadlessSseEvent(event)              │
│                      │                                  │
└──────────────────────┼──────────────────────────────────┘
                       │ page.exposeFunction
                       ▼
┌─────────────────────────────────────────────────────────┐
│                    Java 回调                              │
│                                                         │
│  HeadlessSseEventParser.parseEvent(eventJson)           │
│       → AguiEvent 子类型                                 │
│       → HeadlessAgentListener.onXxx()  实时回调          │
│       → SseEventCollector.addEvent()  收集汇总           │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 4.2 fetch 拦截器 JS 脚本

关键要点：

1. **`response.body.tee()`**：克隆 ReadableStream，一份给前端正常消费，一份给拦截器解析
2. **SSE 解析**：与 `@ag-ui/client` 的 `parseSSEStream` 逻辑一致
   - 用 `TextDecoder` 解码 Uint8Array 为 UTF-8 字符串
   - 按 `\n\n` 分割为独立事件块
   - 每个块按 `\n` 分行，提取 `data:` 开头的行
   - 多行 `data` 拼接后为一个完整 JSON
3. **`window.__onHeadlessSseEvent`**：通过 `page.exposeFunction` 映射到 Java 回调
4. **仅拦截 `/brain/run/copilotKit`**：其他 fetch 请求不受影响

### 4.3 注入时机

- 每次创建新的 BrowserContext / Page 时注入
- 在 `ensureFetchInterceptor()` 中检查是否已注入（幂等）
- 如果页面导航导致 JS 上下文重置，重新注入

## 5. 审批/提问处理流程

### 5.1 自动处理（有 HeadlessApprovalHandler）

```
SSE: CUSTOM(name=HUMAN_APPROVAL) 事件
  → parser 解析为 AguiEvent.Custom
  → listener.onHumanApproval(event)          // 通知业务端
  → approvalHandler.handleApproval(event)     // 获取审批决策
  → 根据决策操作界面：
    - APPROVED → 点击"批准"按钮
    - REJECTED → 点击"拒绝"按钮 + 填写原因（如有）
  → 助手继续运行
```

```
SSE: TOOL_CALL_END(toolCallName=AskUserQuestion) 事件
  → parser 解析为 AguiEvent.ToolCallEnd
  → listener.onAskUserQuestion(event)              // 通知业务端
  → approvalHandler.handleAskUserQuestion(event)    // 获取答案
  → 根据答案操作界面：
    - 点击对应选项
    - 点击"提交答案"按钮
  → 助手继续运行
```

### 5.2 手动处理（无 HeadlessApprovalHandler）

```
SSE: CUSTOM(name=HUMAN_APPROVAL) 事件
  → parser 解析为 AguiEvent.Custom
  → listener.onHumanApproval(event)          // 通知业务端
  → 创建 CompletableFuture 等待
  → 业务端调用 session.approve() 或 session.reject(reason)
  → CompletableFuture 完成
  → 操作界面审批按钮
  → 助手继续运行
```

## 6. 并发控制

| 机制 | 说明 |
|------|------|
| `Semaphore(maxContexts)` | 限制同时存在的 BrowserContext 数量，默认 30 |
| `ConcurrentHashMap<userId, Session>` | 用户级 Session 隔离，同一用户复用 |
| 闲置超时回收 | 每 1 分钟扫描，超过 10 分钟未访问的 Session 自动关闭并释放 Semaphore 许可 |
| `CountDownLatch` | 每次 `sendMessage` 内部使用，确保 RUN_FINISHED 后才返回 |

### 资源预估

| 资源 | 单实例占用 | 30 并发总计 |
|------|-----------|------------|
| Browser 进程 | ~80MB | ~80MB（共享） |
| BrowserContext | ~2MB | ~60MB |
| Page | ~20MB | ~600MB |
| **合计** | - | **~740MB** |

## 7. 配置项

```yaml
# application.yaml
headless:
  browser:
    max-contexts: 30           # 最大并发 BrowserContext 数量
    idle-timeout-ms: 600000    # 闲置超时 10 分钟
    sse-timeout-ms: 300000     # SSE 等待超时 5 分钟
    headless: true             # 是否无头模式
    base-url: http://localhost:8000  # 应用基础 URL
```

## 8. 使用示例

```java
@RestController
@RequestMapping("test/headless")
public class HeadlessTestController {

    private final HeadlessBrowserManager manager;

    @PostMapping("chat")
    public R<List<AguiEvent>> chat(
            @RequestParam String userId,
            @RequestParam String message) {

        StringBuilder reply = new StringBuilder();

        List<AguiEvent> events = manager.sendMessage(
            userId,
            message,
            new HeadlessAgentListener() {
                @Override
                public void onTextMessageContent(String delta) {
                    reply.append(delta);
                }

                @Override
                public void onHumanApproval(AguiEvent.Custom event) {
                    log.info("收到审批请求: {}", event.value());
                }
            },
            event -> HeadlessApprovalHandler.ApprovalResult.accept()  // 自动批准
        );

        return R.ok(events);
    }
}
```
