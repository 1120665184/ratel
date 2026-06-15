package org.quyq.gwsu;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Playwright 用法示例测试类
 * 
 * 涵盖以下核心能力：
 * 1. 监听接口响应（包括 SSE 流式响应）
 * 2. 界面操作（点击按钮、文本输入、等待元素等）
 * 3. 并发调用的最佳实践
 * 
 * 架构说明：
 * - Browser：浏览器进程，全局共享一个即可，约 50-100MB 内存
 * - BrowserContext：隔离的浏览器会话，有独立的 cookies/storage，约 1-2MB，轻量级
 * - Page：浏览器标签页，约 10-30MB，是主要资源消耗点
 * 
 * 并发原则：
 * - Browser 可多线程共享（线程安全）
 * - BrowserContext 和 Page 不是线程安全的，每个并发任务需独立实例
 * - 不同用户必须使用独立的 BrowserContext（登录态隔离）
 * 
 * @author Quyq
 */
public class PlaywrightDemoTest {

    /** Playwright 实例，管理浏览器驱动 */
    private static Playwright playwright;

    /** 
     * Browser 实例，代表一个浏览器进程
     * 全局共享一个 Browser，所有 Context 都复用这个进程
     */
    private static Browser browser;

    /** 
     * BrowserContext 实例，代表一个隔离的浏览器会话
     * 每个测试方法独立的 Context，确保测试间互不干扰
     */
    private BrowserContext context;

    /** 
     * Page 实例，代表一个浏览器标签页
     * 每个测试方法独立的 Page
     */
    private Page page;

    /**
     * 在所有测试前启动浏览器（只执行一次）
     * 
     * LaunchOptions 配置：
     * - headless: true - 无头模式，不显示浏览器窗口，适合 CI/CD
     * - slowMo: 100 - 每个操作延迟 100ms，便于观察（调试时可设为 0）
     */
    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setSlowMo(100)
        );
    }

    /**
     * 在所有测试后关闭浏览器（只执行一次）
     * 释放 Playwright 和 Browser 资源
     */
    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    /**
     * 在每个测试前创建独立的 Context 和 Page
     * 
     * NewContextOptions 配置：
     * - viewportSize: 设置视口大小，模拟桌面浏览器
     * 
     * 可扩展配置：
     * - setLocale("zh-CN") - 设置语言
     * - setGeolocation() - 设置地理位置
     * - setPermissions() - 设置权限（如通知、摄像头等）
     */
    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
        );
        page = context.newPage();
    }

    /**
     * 在每个测试后关闭 Context
     * Context 关闭后，其下的所有 Page 也会自动关闭
     * 同时清除 cookies、localStorage 等会话数据
     */
    @AfterEach
    void closeContext() {
        context.close();
    }

    // ==================== 接口监听能力 ====================

    /**
     * 示例：监听所有 HTTP 响应
     * 
     * page.onResponse() 注册一个回调，在每次网络请求完成时触发
     * 可用于：
     * - 监控所有 API 调用
     * - 记录请求日志
     * - 检查接口是否被正确调用
     */
    @Test
    void demoListenHttpResponse() {
        // 使用线程安全的 List 收集响应（并发场景下需要）
        List<Response> apiResponses = Collections.synchronizedList(new ArrayList<>());

        // 注册响应监听器，捕获所有 /api/ 开头的请求
        page.onResponse(response -> {
            if (response.url().contains("/api/")) {
                apiResponses.add(response);
                System.out.printf("[接口响应] %s %s → %d%n",
                        response.request().method(), response.url(), response.status());
            }
        });

        // 导航到页面，触发网络请求
        page.navigate("http://localhost:8000");

        // 验证至少捕获到一个 API 响应
        assertFalse(apiResponses.isEmpty(), "应该捕获到至少一个API响应");
    }

    /**
     * 示例：监听指定接口并提取响应体
     * 
     * 通过 URL 和状态码过滤特定接口
     * 使用 response.text() 获取响应体内容
     * 
     * 适用场景：
     * - 验证特定接口返回的数据
     * - 检查接口响应是否符合预期
     */
    @Test
    void demoListenSpecificApiAndExtractBody() {
        AtomicReference<String> responseBody = new AtomicReference<>();

        // 只监听 /api/user/info 接口且状态码为 200
        page.onResponse(response -> {
            if (response.url().contains("/api/user/info") && response.status() == 200) {
                try {
                    // 获取响应体文本
                    responseBody.set(response.text());
                    System.out.printf("[指定接口] /api/user/info → %s%n", response.text());
                } catch (Exception e) {
                    System.err.println("读取响应体失败: " + e.getMessage());
                }
            }
        });

        page.navigate("https://example.com/dashboard");

        assertNotNull(responseBody.get(), "应该捕获到 /api/user/info 的响应体");
    }

    /**
     * 示例：监听 SSE（Server-Sent Events）响应
     * 
     * SSE 是服务器向客户端推送事件的流式协议
     * 特点：单向推送、基于 HTTP、自动重连
     * 
     * 识别 SSE：content-type 包含 "text/event-stream"
     * 
     * 注意：在 onResponse 回调中读取 SSE body 可能只拿到部分数据
     * 因为 SSE 是长连接流式推送，响应可能尚未完成
     * 更可靠的方式见 demoListenSSEByPageEvaluate()
     */
    @Test
    void demoListenSSEResponse() {
        ConcurrentLinkedQueue<String> sseEvents = new ConcurrentLinkedQueue<>();

        page.onResponse(response -> {
            String contentType = response.headers().get("content-type");
            // SSE 响应的 content-type 是 text/event-stream
            if (contentType != null && contentType.contains("text/event-stream")) {
                System.out.printf("[SSE连接] %s%n", response.url());

                try {
                    // 读取响应体（注意：可能只拿到部分数据）
                    byte[] body = response.body();
                    String bodyText = new String(body, StandardCharsets.UTF_8);
                    
                    // 解析 SSE 格式：每个事件以 \n\n 分隔，数据行以 data: 开头
                    for (String line : bodyText.split("\n\n")) {
                        if (line.startsWith("data:")) {
                            String data = line.substring("data:".length()).trim();
                            sseEvents.add(data);
                            System.out.printf("[SSE事件] %s%n", data);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("读取SSE响应体失败: " + e.getMessage());
                }
            }
        });

        page.navigate("https://example.com/chat");

        // 等待 SSE 事件推送
        page.waitForTimeout(5000);

        System.out.printf("[SSE] 共收到 %d 个事件%n", sseEvents.size());
    }

    /**
     * 示例：通过 page.evaluate() 在浏览器内消费 SSE（推荐方式）
     * 
     * 原理：在浏览器上下文中执行 JavaScript，使用 EventSource API 直接消费 SSE
     * 优点：
     * - 能完整接收所有 SSE 事件
     * - 不受 Playwright onResponse 回调时机限制
     * - 更接近真实用户场景
     * 
     * 适用场景：
     * - 需要完整收集 SSE 推送的所有数据
     * - 验证 SSE 事件的顺序和内容
     */
    @Test
    void demoListenSSEByPageEvaluate() {
        // 仍然可以监听 SSE 连接建立（用于日志记录）
        page.onResponse(response -> {
            String contentType = response.headers().get("content-type");
            if (contentType != null && contentType.contains("text/event-stream")) {
                System.out.printf("[SSE连接捕获] %s%n", response.url());
            }
        });

        page.navigate("https://example.com/chat");

        // 在浏览器内执行 JavaScript，使用 EventSource API 消费 SSE
        // 返回一个 Promise，收集 5 个事件后关闭连接
        Object sseData = page.evaluate("() => {" +
                "  return new Promise((resolve) => {" +
                "    const events = [];" +
                "    const source = new EventSource('/api/chat/stream');" +
                "    source.onmessage = (e) => {" +
                "      events.push(e.data);" +
                "      if (events.length >= 5) {" +           // 收集 5 个事件后关闭
                "        source.close();" +
                "        resolve(events);" +
                "      }" +
                "    };" +
                "    source.onerror = () => { source.close(); resolve(events); };" +
                "    setTimeout(() => { source.close(); resolve(events); }, 10000);" + // 超时保护
                "  });" +
                "}");

        System.out.printf("[SSE通过evaluate] 收到数据: %s%n", sseData);
    }

    // ==================== 界面操作能力 ====================

    /**
     * 示例：点击按钮的多种方式
     * 
     * Playwright 提供多种定位元素的方式：
     * 1. CSS 选择器：page.locator("button[type='submit']")
     * 2. 文本匹配：page.locator("button:has-text('确定')")
     * 3. ARIA 角色：page.getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName("保存"))
     * 
     * 推荐优先使用 ARIA 角色定位，更符合无障碍标准，也更稳定
     */
    @Test
    void demoClickButton() {
        page.navigate("https://example.com/login");

        // 方式1：CSS 选择器定位
        Locator submitBtn = page.locator("button[type='submit']");
        submitBtn.click();

        // 方式2：组合选择器 + 文本匹配
        // 在 #confirm-dialog 内找到包含 "确定" 文本的 button
        page.locator("#confirm-dialog button:has-text('确定')").click();

        // 方式3：ARIA 角色定位（推荐）
        // 通过按钮的无障碍角色和名称定位，更稳定
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("保存")).click();
    }

    /**
     * 示例：文本输入的多种方式
     * 
     * fill() - 直接填入文本，不触发键盘事件（适合表单填写）
     * pressSequentially() - 逐字符输入，触发完整的键盘事件（模拟真实用户输入）
     * clear() - 清空输入框
     * selectOption() - 选择下拉框选项
     * check()/uncheck() - 勾选/取消勾选复选框
     */
    @Test
    void demoTextInput() {
        page.navigate("https://example.com/form");

        // fill(): 直接填入文本，快速但不触发逐字符键盘事件
        page.locator("#username").fill("admin");

        page.locator("#password").fill("password123");

        // pressSequentially(): 逐字符输入，模拟真实键盘输入
        // setDelay(50) 设置每个字符间隔 50ms
        // 适用场景：需要触发 input 事件、有实时校验的输入框
        page.locator("#search").pressSequentially("关键词", new Locator.PressSequentiallyOptions().setDelay(50));

        page.locator("#description").pressSequentially("这是一段描述文字",
                new Locator.PressSequentiallyOptions().setDelay(30));

        // 先清空再填入新值
        page.locator("#email").clear();
        page.locator("#email").fill("new@example.com");

        // 下拉框选择
        Locator select = page.locator("#role");
        select.selectOption("admin");  // 通过 value 选择
        // 也可以通过文本选择：select.selectOption(new SelectOptionOptions().setLabel("管理员"));

        // 复选框勾选
        page.locator("#agree").check();

        // 复选框取消勾选
        page.locator("#enabled").uncheck();
    }

    /**
     * 示例：等待元素出现的多种方式
     * 
     * waitFor() - 等待元素达到指定状态
     * waitForSelector() - 等待选择器匹配的元素出现
     * 
     * WaitForSelectorState 状态：
     * - VISIBLE: 元素可见（在视口内且不隐藏）
     * - HIDDEN: 元素隐藏或不存在
     * - ATTACHED: 元素存在于 DOM 中（可能不可见）
     * - DETACHED: 元素从 DOM 中移除
     */
    @Test
    void demoWaitForElement() {
        page.navigate("https://example.com/dashboard");

        // 等待加载动画消失（等待元素变为 HIDDEN 状态）
        page.locator(".loading-spinner").waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));

        // 等待数据表格可见
        page.locator(".data-table").waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // 等待元素挂载到 DOM（即使不可见）
        page.locator("#status").waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));

        // 简化写法：等待元素出现（默认等待 VISIBLE）
        page.waitForSelector(".notification");

        // Locator 的 waitFor() 默认也是等待元素存在
        page.locator("#dynamic-content").waitFor();
    }

    /**
     * 示例：等待特定 API 响应
     * 
     * waitForResponse() - 执行操作并等待特定响应返回
     * 
     * 适用场景：
     * - 点击按钮后等待数据加载完成
     * - 确保操作触发了正确的 API 调用
     * - 获取 API 响应数据进行验证
     */
    @Test
    void demoWaitForApiResponse() {
        page.navigate("https://example.com/users");

        // 执行点击操作，同时等待 /api/users 响应
        // waitForResponse 的第二个参数是触发请求的操作（Runnable）
        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/users") && resp.status() == 200,
                () -> {
                    page.locator("#refresh-btn").click();
                }
        );

        // 获取响应体
        String body = response.text();
        System.out.printf("[等待API] 响应体: %s%n", body);
        assertNotNull(body);
    }

    // ==================== 并发调用能力 ====================

    /**
     * 示例：多任务并发执行（ExecutorService + 独立 Context）
     * 
     * 并发最佳实践：
     * 1. 每个并发任务使用独立的 BrowserContext（隔离登录态）
     * 2. 每个任务使用独立的 Page（Page 不是线程安全的）
     * 3. 使用 CountDownLatch 或 CompletableFuture 同步等待所有任务完成
     * 4. 任务完成后及时关闭 Context 和 Page 释放资源
     * 
     * 适用场景：
     * - 多用户并发测试
     * - 批量页面操作
     * - 性能压测
     */
    @Test
    void demoConcurrentMultiplePages() throws Exception {
        int taskCount = 5;
        // 固定大小线程池，控制并发上限
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        // 用于等待所有任务完成
        CountDownLatch latch = new CountDownLatch(taskCount);
        // 线程安全的结果收集器
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < taskCount; i++) {
            final int index = i;
            executor.submit(() -> {
                BrowserContext ctx = null;
                Page pg = null;
                try {
                    // 每个任务独立的 Context 和 Page
                    ctx = browser.newContext();
                    pg = ctx.newPage();

                    // 监听该任务的 API 响应
                    AtomicReference<String> captured = new AtomicReference<>();
                    pg.onResponse(resp -> {
                        if (resp.url().contains("/api/data")) {
                            try {
                                captured.set(resp.text());
                            } catch (Exception ignored) {
                            }
                        }
                    });

                    // 执行页面操作
                    pg.navigate("https://example.com/page/" + index);
                    pg.locator("#search-input").fill("查询" + index);
                    pg.locator("#search-btn").click();

                    // 等待 API 响应
                    pg.waitForResponse(
                            resp -> resp.url().contains("/api/data"),
                            () -> {}
                    );

                    String result = captured.get();
                    results.add("任务" + index + ": " + (result != null ? "成功" : "无数据"));
                } catch (Exception e) {
                    results.add("任务" + index + ": 异常 - " + e.getMessage());
                } finally {
                    // 必须在 finally 中关闭，确保异常时也能释放资源
                    if (pg != null) pg.close();
                    if (ctx != null) ctx.close();
                    latch.countDown();
                }
            });
        }

        // 等待所有任务完成，最多 30 秒
        assertTrue(latch.await(30, TimeUnit.SECONDS), "所有任务应在30秒内完成");
        executor.shutdown();

        results.forEach(System.out::println);
        assertEquals(taskCount, results.size(), "应完成所有任务");
    }

    /**
     * 示例：使用 CompletableFuture 实现并发（更现代的写法）
     * 
     * 优点：
     * - try-with-resources 自动关闭 Context 和 Page
     * - CompletableFuture.allOf() 聚合所有异步任务
     * - 代码更简洁，异常处理更清晰
     * 
     * 适用场景：
     * - 需要异步执行并收集结果
     * - 需要对结果进行后续处理
     */
    @Test
    void demoConcurrentWithCompletableFuture() throws Exception {
        int taskCount = 3;
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            final int index = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                // try-with-resources 自动关闭资源
                try (BrowserContext ctx = browser.newContext();
                     Page pg = ctx.newPage()) {

                    AtomicReference<String> apiResult = new AtomicReference<>();
                    pg.onResponse(resp -> {
                        if (resp.url().contains("/api/result")) {
                            try {
                                apiResult.set(resp.text());
                            } catch (Exception ignored) {
                            }
                        }
                    });

                    pg.navigate("https://example.com/task/" + index);
                    pg.locator("#input").fill("输入" + index);
                    pg.locator("#submit").click();

                    pg.waitForResponse(
                            resp -> resp.url().contains("/api/result"),
                            () -> {}
                    );

                    return "任务" + index + "完成: " + apiResult.get();
                }
            }));
        }

        // 聚合所有任务，等待全部完成
        CompletableFuture<Void> all = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        all.get(30, TimeUnit.SECONDS);

        // 获取每个任务的结果
        for (CompletableFuture<String> f : futures) {
            System.out.println(f.get());
        }
    }

    /**
     * 示例：单页面内的串行操作（同一用户多步骤）
     * 
     * 场景：同一页面内切换多个 Tab，每个操作等待对应 API 返回
     * 
     * 特点：
     * - 共享同一个 Page（串行操作，无并发冲突）
     * - 每个操作后 waitForResponse 确保数据加载完成
     * - 所有 API 响应被统一收集
     */
    @Test
    void demoConcurrentSamePageMultipleActions() {
        page.navigate("https://example.com/dashboard");

        // 收集所有 API 响应
        List<Response> responses = Collections.synchronizedList(new ArrayList<>());
        page.onResponse(resp -> {
            if (resp.url().contains("/api/")) {
                responses.add(resp);
            }
        });

        // 串行切换 Tab，每次等待对应 API 返回
        page.locator("#tab-users").click();
        page.waitForResponse(resp -> resp.url().contains("/api/users"), () -> {});

        page.locator("#tab-orders").click();
        page.waitForResponse(resp -> resp.url().contains("/api/orders"), () -> {});

        page.locator("#tab-settings").click();
        page.waitForResponse(resp -> resp.url().contains("/api/settings"), () -> {});

        System.out.printf("[串行操作] 共捕获 %d 个API响应%n", responses.size());
    }

    // ==================== 综合示例 ====================

    /**
     * 示例：完整业务流程（登录 + SSE 聊天）
     * 
     * 综合演示：
     * - 监听多种类型的接口（普通 HTTP + SSE）
     * - 界面操作（输入、点击）
     * - 等待页面跳转和元素出现
     * - 等待 API 响应
     * 
     * 适用场景：
     * - E2E 测试完整业务流程
     * - 验证多步骤交互的正确性
     */
    @Test
    void demoFullWorkflow() {
        // 收集 SSE 消息和登录响应
        ConcurrentLinkedQueue<String> sseMessages = new ConcurrentLinkedQueue<>();
        AtomicReference<String> loginResponse = new AtomicReference<>();

        // 统一监听所有响应
        page.onResponse(response -> {
            String contentType = response.headers().get("content-type");

            // 监听登录接口
            if (response.url().contains("/api/auth/login")) {
                try {
                    loginResponse.set(response.text());
                    System.out.printf("[登录接口] 状态: %d%n", response.status());
                } catch (Exception ignored) {
                }
            }

            // 监听 SSE 连接
            if (contentType != null && contentType.contains("text/event-stream")) {
                System.out.printf("[SSE连接] %s%n", response.url());
            }

            // 监听发送消息接口
            if (response.url().contains("/api/chat/send") && response.status() == 200) {
                System.out.printf("[发送消息接口] 响应: %d%n", response.status());
            }
        });

        // 步骤1：登录
        page.navigate("https://example.com/login");
        page.locator("#username").fill("admin");
        page.locator("#password").fill("password123");
        page.locator("button[type='submit']").click();

        // 等待跳转到 dashboard
        page.waitForURL("**/dashboard");

        // 步骤2：发送聊天消息
        page.locator("#chat-input").fill("你好，请帮我查询数据");
        page.locator("#send-btn").click();

        // 等待发送消息的 API 响应
        page.waitForResponse(
                resp -> resp.url().contains("/api/chat/send"),
                () -> page.locator("#send-btn").click()
        );

        // 等待聊天响应出现
        page.waitForSelector(".chat-response", new Page.WaitForSelectorOptions().setTimeout(10000));

        // 输出结果
        System.out.printf("[完整流程] 登录响应: %s%n", loginResponse.get());
        System.out.printf("[完整流程] SSE消息数: %d%n", sseMessages.size());
    }
}