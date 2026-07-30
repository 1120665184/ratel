# Headless Playwright 流式触发修复设计

## 目标

使无头浏览器能够发送包含多模态资源的消息，同时避免一次 Agent 流期间对同一 Playwright `Page` 的并发访问。

## 问题与根因

当前 Java 端通过 `page.evaluate()` 调用前端 bridge，并直接返回 `bridge.send(payload)`。
bridge 的 `send` 会等待 `copilotkit.runAgent({ agent })`，因此 Playwright 的同步调用会持续到整段 SSE 流结束。期间 Redis 事件消费者收到 `RunStarted` 后启动录屏，录屏线程和 `AGENT_OUTPUT_END` 处理会执行 `page.screenshot()`。这与尚未返回的 `page.evaluate()` 并发操作同一 `Page`，违反 Java Playwright 的线程安全要求，触发 `Object doesn't exist: response@...`。

## 设计

### 前端 bridge

bridge 的职责调整为仅启动 Agent：同步构建多模态消息、写入 agent 消息队列、发起 `runAgent`，但不将其 Promise 暴露给 Java `page.evaluate()`。后台 Promise 被捕获并输出带 `[Headless]` 前缀的控制台错误，避免未处理拒绝。

### 后端触发与 SSE 衔接

`triggerAssistant` 通过 `page.waitForRequest` 包裹 bridge 调用，等待 `/brain/run/copilotKit` 的 `agent/run` 请求已经发出。现有 `page.route` 会在该阶段提取 threadId 并启动 Redis SSE 消费器；随后才进入 `collector.awaitCompletion()`。这既不等待整段 Agent 流，也不会在路由尚未建立时阻塞在 Java 的 `CountDownLatch`。

### Playwright 串行化

在 `HeadlessBrowserSession` 内维护一个会话级、可重入的 Playwright 操作锁。所有该 Session 所有权范围内的 `Page` 操作都经该锁进行：发送、认证、隐藏表单、关闭，以及 `HeadlessPageWrapper` 的页面检查和截图。录屏线程和 SSE 消费线程可继续存在，但调用页面 API 时会按锁串行，不与触发调用交错。

为避免锁跨越整段业务等待，`sendMessage` 仅对浏览器触发步骤持锁；SSE 的 Redis 等待不持锁。页面截图各自短暂持锁。

## 错误处理

- bridge 发起失败：前端记录 `[Headless]` 控制台错误；后端不会再因为等待长期 Promise 而卡住。
- 30 秒内未观察到 `agent/run` 请求：`triggerAssistant` 失败并沿用现有错误传播链。
- Page 已关闭：截图保持当前的降级行为，不影响会话收尾。

## 验证

1. 前端单元测试验证 bridge 调用不会返回 `runAgent` Promise，并会处理其拒绝。
2. 后端单元测试验证发送逻辑等待 `agent/run` 请求，而不依赖完整流结束。
3. 编译对应 Maven 模块与前端应用 TypeScript 检查。
4. 本机发送一条纯文本和一条带资源消息，确认不再出现 Playwright `response@` 异常，且录屏/截图仍可生成。
