# TOOL_EXECUTE 工具执行框架实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现后端 AG-UI CUSTOM 事件 TOOL_EXECUTE 的前端工具执行框架，支持后端等待前端结果并恢复 Agent 推理。

**Architecture:** 后端 WebToolUtils.webExecuteTool() 发送 CUSTOM 事件并通过 Redisson 信号量阻塞等待前端结果；前端通过 CopilotKit agent.subscribe 的 onCustomEvent 监听 CUSTOM 事件，分发到注册的工具执行器执行，完成后回调后端接口释放信号量。

**Tech Stack:** Java 25 / Spring Boot 4.0.3 / Redisson 4.3.0 / React 18 / TypeScript 5 / CopilotKit 1.56.2

---

## 文件变更清单

### 后端 - 新建文件
| 文件 | 职责 |
|------|------|
| `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolType.java` | 工具类型枚举（AUTO/INTERACTIVE） |
| `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolStatus.java` | 工具任务状态枚举（PENDING/SUCCESS/FAILED/TIMEOUT） |
| `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolTask.java` | Redis 存储的任务 record |
| `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolCallbackRequest.java` | 前端回调请求 DTO |

### 后端 - 修改文件
| 文件 | 变更 |
|------|------|
| `common/common-ai/pom.xml` | 新增 common-cache 依赖 |
| `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolExecuteHook.java` | WebToolInfo 移出为独立 record，增加 toolCallId 和 toolType |
| `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/utils/WebToolUtils.java` | 重写 webExecuteTool()：生成 toolCallId、Redis 存储、Redisson 信号量等待、返回结果 |
| `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/AguiController.java` | 新增 toolCallback 通用方法 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/controller/BrainController.java` | 新增 tool/callback 接口路由 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/service/tool/WebTool.java` | 改用新的 webExecuteTool 签名，直接返回结果 |

### 前端 - 新建文件
| 文件 | 职责 |
|------|------|
| `web/apps/gwsu-main/src/services/web-tool/types.ts` | WebTool 类型定义 |
| `web/apps/gwsu-main/src/services/web-tool/registry.ts` | 工具注册表 |
| `web/apps/gwsu-main/src/services/web-tool/dispatcher.ts` | 工具分发器 + 回调逻辑 |
| `web/apps/gwsu-main/src/services/web-tool/index.ts` | 统一导出 |
| `web/apps/gwsu-main/src/services/web-tool/tools/route-navigation.ts` | 路由跳转工具实现 |

### 前端 - 修改文件
| 文件 | 变更 |
|------|------|
| `web/apps/gwsu-main/src/services/brain.ts` | 新增 toolCallback API 函数 |
| `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx` | 订阅 agent 的 CUSTOM 事件，分发到 WebToolDispatcher |

---

### Task 1: 后端 - 新增 WebToolType 枚举

**Files:**
- Create: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolType.java`

- [ ] **Step 1: 创建 WebToolType 枚举**

```java
package org.quyq.gwsu.common.ai.agui.web;

/**
 * Web工具类型枚举
 */
public enum WebToolType {
    /** 自动执行，无需用户交互 */
    AUTO,
    /** 需要用户交互确认 */
    INTERACTIVE
}
```

- [ ] **Step 2: 创建 WebToolStatus 枚举**

Create: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolStatus.java`

```java
package org.quyq.gwsu.common.ai.agui.web;

/**
 * Web工具任务状态枚举
 */
public enum WebToolStatus {
    /** 等待前端执行 */
    PENDING,
    /** 执行成功 */
    SUCCESS,
    /** 执行失败 */
    FAILED,
    /** 执行超时 */
    TIMEOUT
}
```

- [ ] **Step 3: 创建 WebToolTask record**

Create: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolTask.java`

```java
package org.quyq.gwsu.common.ai.agui.web;

import java.util.Map;

/**
 * Web工具执行任务，存储在Redis中
 * @param toolCallId 工具调用唯一标识
 * @param toolName 工具名称
 * @param toolType 工具类型
 * @param params 工具参数
 * @param status 执行状态
 * @param result 执行结果
 */
public record WebToolTask(
        String toolCallId,
        String toolName,
        WebToolType toolType,
        Map<String, Object> params,
        WebToolStatus status,
        String result
) {

    /**
     * 创建初始任务（PENDING状态）
     */
    public static WebToolTask pending(String toolCallId, String toolName, WebToolType toolType, Map<String, Object> params) {
        return new WebToolTask(toolCallId, toolName, toolType, params, WebToolStatus.PENDING, null);
    }

    /**
     * 更新状态和结果
     */
    public WebToolTask withResult(WebToolStatus status, String result) {
        return new WebToolTask(toolCallId, toolName, toolType, params, status, result);
    }
}
```

- [ ] **Step 4: 创建 WebToolCallbackRequest DTO**

Create: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolCallbackRequest.java`

```java
package org.quyq.gwsu.common.ai.agui.web;

/**
 * 前端工具执行结果回调请求
 * @param toolCallId 工具调用唯一标识
 * @param success 是否执行成功
 * @param result 执行结果描述
 */
public record WebToolCallbackRequest(
        String toolCallId,
        boolean success,
        String result
) {
}
```

- [ ] **Step 5: Commit**

```bash
git add common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/
git commit -m "feat: 新增WebTool数据模型（WebToolType/WebToolStatus/WebToolTask/WebToolCallbackRequest）"
```

---

### Task 2: 后端 - 重构 WebToolExecuteHook，WebToolInfo 移出为独立 record

**Files:**
- Modify: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolExecuteHook.java`

- [ ] **Step 1: 将 WebToolInfo 从 WebToolExecuteHook 内部 record 移出为独立文件，增加 toolCallId 和 toolType**

Create: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/WebToolInfo.java`

```java
package org.quyq.gwsu.common.ai.agui.web;

import java.util.Map;

/**
 * Web工具执行信息，通过CUSTOM事件发送给前端
 * @param toolCallId 工具调用唯一标识
 * @param toolName 工具名称
 * @param toolType 工具类型
 * @param params 工具参数
 */
public record WebToolInfo(
        String toolCallId,
        String toolName,
        WebToolType toolType,
        Map<String, Object> params
) {
}
```

- [ ] **Step 2: 修改 WebToolExecuteHook，移除内部 WebToolInfo record，改用独立类**

修改 `WebToolExecuteHook.java`：

```java
package org.quyq.gwsu.common.ai.agui.web;


import com.google.gson.Gson;
import io.agentscope.core.agui.encoder.AguiEventEncoder;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.hook.ActingChunkEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.AguiController;
import org.quyq.gwsu.common.ai.agui.domain.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Objects;

/**
 * 监听是否有需要浏览器端执行的工具事件，有的话发送给浏览器
 */
@Slf4j
public class WebToolExecuteHook implements Hook {

    private final String threadId;

    public WebToolExecuteHook(String threadId) {
        this.threadId = threadId;
    }

    private final AguiEventEncoder encoder = new AguiEventEncoder();

    private final Gson gson = new Gson();

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof ActingChunkEvent e && !CollectionUtils.isEmpty(e.getChunk().getOutput())) {
            ContentBlock block = e.getChunk().getOutput().getFirst();
            if (block instanceof TextBlock t && t.getText().startsWith(WebToolUtils.WEB_TOOL_IDENTIFICATION)) {

                WebToolInfo info = gson.fromJson(
                        t.getText().replace(WebToolUtils.WEB_TOOL_IDENTIFICATION, ""),
                        WebToolInfo.class
                );
                AIRunnerInstanceWrapper sseEmitter = AguiController.getCurrEmitter(threadId);
                if (Objects.isNull(sseEmitter)) {
                    return Mono.just(event);
                }

                //发送工具执行自定义事件
                AguiEvent.Custom customAguiEvent = new AguiEvent.Custom(threadId, sseEmitter.input().getRunId(), "TOOL_EXECUTE", info);
                sendEvent(sseEmitter.emitter(), customAguiEvent);
            }

        }

        return Mono.just(event);
    }

    private void sendEvent(SseEmitter emitter, AguiEvent event) {
        try {
            String jsonData = encoder.encodeToJson(event);
            emitter.send(SseEmitter.event().data(jsonData, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.debug("Failed to send SSE event: {}", e.getMessage());
        }
    }
}
```

关键变更：删除内部 `WebToolInfo` record，改用同包下的独立 `WebToolInfo` 类。变量名 `black` 改为 `block`。

- [ ] **Step 3: Commit**

```bash
git add common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/web/
git commit -m "refactor: WebToolInfo移出为独立record，增加toolCallId和toolType字段"
```

---

### Task 3: 后端 - common-ai 添加 common-cache 依赖

**Files:**
- Modify: `common/common-ai/pom.xml`

- [ ] **Step 1: 在 common-ai 的 pom.xml 中添加 common-cache 依赖**

在 `<dependencies>` 中 `common-core` 依赖之后添加：

```xml
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>common-cache</artifactId>
        </dependency>
```

- [ ] **Step 2: 验证依赖可以解析**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn dependency:resolve -pl common/common-ai -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add common/common-ai/pom.xml
git commit -m "feat: common-ai添加common-cache依赖（引入Redisson）"
```

---

### Task 4: 后端 - 重写 WebToolUtils.webExecuteTool()

**Files:**
- Modify: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/utils/WebToolUtils.java`

- [ ] **Step 1: 重写 WebToolUtils，集成 Redisson 信号量等待**

```java
package org.quyq.gwsu.common.ai.agui.utils;


import com.google.gson.Gson;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolEmitter;
import org.quyq.gwsu.common.ai.agui.web.WebToolInfo;
import org.quyq.gwsu.common.ai.agui.web.WebToolStatus;
import org.quyq.gwsu.common.ai.agui.web.WebToolTask;
import org.quyq.gwsu.common.ai.agui.web.WebToolType;
import org.redisson.api.RBucket;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Web工具执行工具类
 * 封装：生成toolCallId → 发送CUSTOM事件 → Redis存储 → Redisson信号量等待 → 返回结果
 */
public class WebToolUtils {

    private WebToolUtils() {
    }

    public static final String WEB_TOOL_IDENTIFICATION = "NOTICE_WEB_TOOL:";

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "gwsu:web-tool:";

    /** 信号量 key 前缀 */
    private static final String SEMAPHORE_PREFIX = "gwsu:web-tool:sem:";

    /** 默认超时时间（秒） */
    private static final long DEFAULT_TIMEOUT_SECONDS = 60;

    /** Redis TTL（秒），大于超时时间确保回调时数据仍存在 */
    private static final long REDIS_TTL_SECONDS = 120;

    private static final Gson gson = new Gson();

    /**
     * 通知web端执行指定工具，阻塞等待前端结果后返回
     *
     * @param toolEmitter 工具发射器
     * @param toolName    工具名称
     * @param params      工具参数
     * @param toolType    工具类型
     * @return 工具执行结果
     */
    public static ToolResultBlock webExecuteTool(ToolEmitter toolEmitter, String toolName,
                                                 Map<String, Object> params, WebToolType toolType) {
        return webExecuteTool(toolEmitter, toolName, params, toolType, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 通知web端执行指定工具，阻塞等待前端结果后返回
     *
     * @param toolEmitter    工具发射器
     * @param toolName       工具名称
     * @param params         工具参数
     * @param toolType       工具类型
     * @param timeoutSeconds 超时时间（秒）
     * @return 工具执行结果
     */
    public static ToolResultBlock webExecuteTool(ToolEmitter toolEmitter, String toolName,
                                                 Map<String, Object> params, WebToolType toolType,
                                                 long timeoutSeconds) {
        RedissonClient redissonClient = getRedissonClient();
        String toolCallId = UUID.randomUUID().toString();

        // 1. 发送CUSTOM事件给前端（通过ToolEmitter → Hook → SSE）
        WebToolInfo info = new WebToolInfo(toolCallId, toolName, toolType, params);
        toolEmitter.emit(ToolResultBlock.text(WEB_TOOL_IDENTIFICATION + gson.toJson(info)));

        // 2. Redis存储任务信息
        RBucket<WebToolTask> bucket = redissonClient.getBucket(KEY_PREFIX + toolCallId);
        bucket.set(WebToolTask.pending(toolCallId, toolName, toolType, params), REDIS_TTL_SECONDS, TimeUnit.SECONDS);

        // 3. Redisson信号量等待前端结果
        RSemaphore semaphore = redissonClient.getSemaphore(SEMAPHORE_PREFIX + toolCallId);
        semaphore.trySetPermits(0);
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResultBlock.text("工具执行被中断: " + toolName);
        }

        if (!acquired) {
            // 超时：更新Redis状态
            WebToolTask task = bucket.get();
            if (task != null) {
                bucket.set(task.withResult(WebToolStatus.TIMEOUT, "执行超时"), 60, TimeUnit.SECONDS);
            }
            return ToolResultBlock.text("工具执行超时: " + toolName);
        }

        // 4. 获取并返回结果
        WebToolTask task = bucket.get();
        if (task == null) {
            return ToolResultBlock.text("工具执行结果丢失: " + toolName);
        }

        if (task.status() == WebToolStatus.SUCCESS) {
            return ToolResultBlock.text(task.result());
        } else {
            return ToolResultBlock.text("工具执行失败: " + task.result());
        }
    }

    /**
     * 获取RedissonClient实例
     * 通过Spring ApplicationContext获取，因为WebToolUtils是静态工具类
     */
    private static RedissonClient getRedissonClient() {
        return RedissonClientHolder.INSTANCE;
    }

    /**
     * 设置RedissonClient实例（由配置类调用）
     */
    public static void setRedissonClient(RedissonClient client) {
        RedissonClientHolder.INSTANCE = client;
    }

    /**
     * RedissonClient持有者，用于静态访问
     */
    private static class RedissonClientHolder {
        static volatile RedissonClient INSTANCE;
    }
}
```

- [ ] **Step 2: 创建 RedissonClient 初始化配置类**

Create: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/config/WebToolRedissonConfig.java`

```java
package org.quyq.gwsu.common.ai.config;

import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * WebTool Redisson配置
 * 应用启动后注入RedissonClient到WebToolUtils
 */
@Configuration
public class WebToolRedissonConfig {

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady(ContextRefreshedEvent event) {
        RedissonClient client = event.getApplicationContext().getBean(RedissonClient.class);
        WebToolUtils.setRedissonClient(client);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/utils/WebToolUtils.java
git add common/common-ai/src/main/java/org/quyq/gwsu/common/ai/config/WebToolRedissonConfig.java
git commit -m "feat: 重写WebToolUtils.webExecuteTool，集成Redisson信号量等待前端结果"
```

---

### Task 5: 后端 - AguiController 新增工具回调方法

**Files:**
- Modify: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/AguiController.java`

- [ ] **Step 1: 在 AguiController 中新增 toolCallback 方法**

在 `AguiController.java` 中添加以下方法（放在 `handleCopilotKitRequest` 方法之后）：

```java
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;
import org.quyq.gwsu.common.ai.agui.web.WebToolStatus;
import org.quyq.gwsu.common.ai.agui.web.WebToolTask;
import org.redisson.api.RBucket;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;

// ... 在类中新增：

    /** Redis key 前缀，与 WebToolUtils 保持一致 */
    private static final String WEB_TOOL_KEY_PREFIX = "gwsu:web-tool:";
    private static final String WEB_TOOL_SEM_PREFIX = "gwsu:web-tool:sem:";

    /**
     * 处理前端工具执行结果回调
     *
     * @param request 回调请求
     * @return 处理结果
     */
    public R<Void> handleToolCallback(WebToolCallbackRequest request, RedissonClient redissonClient) {
        RBucket<WebToolTask> bucket = redissonClient.getBucket(WEB_TOOL_KEY_PREFIX + request.toolCallId());
        WebToolTask task = bucket.get();

        if (task == null) {
            return R.fail("无效的toolCallId: " + request.toolCallId());
        }

        if (task.status() != WebToolStatus.PENDING) {
            return R.fail("工具任务已处理: " + request.toolCallId());
        }

        // 更新状态和结果
        WebToolStatus status = request.success() ? WebToolStatus.SUCCESS : WebToolStatus.FAILED;
        bucket.set(task.withResult(status, request.result()), 60, java.util.concurrent.TimeUnit.SECONDS);

        // 释放信号量，唤醒等待的webExecuteTool()
        RSemaphore semaphore = redissonClient.getSemaphore(WEB_TOOL_SEM_PREFIX + request.toolCallId());
        semaphore.release();

        return R.ok();
    }
```

需要在文件顶部添加 import：
```java
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;
import org.quyq.gwsu.common.ai.agui.web.WebToolStatus;
import org.quyq.gwsu.common.ai.agui.web.WebToolTask;
import org.quyq.gwsu.common.core.domain.R;
import org.redisson.api.RBucket;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
```

- [ ] **Step 2: Commit**

```bash
git add common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/AguiController.java
git commit -m "feat: AguiController新增handleToolCallback方法处理前端工具回调"
```

---

### Task 6: 后端 - BrainController 新增工具回调接口路由

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/controller/BrainController.java`

- [ ] **Step 1: 在 BrainController 中新增工具回调接口**

在 `BrainController` 中添加 `RedissonClient` 注入和回调接口。

修改构造函数，新增 `RedissonClient` 参数：

```java
import org.redisson.api.RedissonClient;
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;

// ... 修改类定义：
public class BrainController {

    private static final String DEFAULT_AGENT_ID_HEADER = "X-Agent-Id";

    private final AguiController aguiController;
    private final IBrainHistoryService brainHistoryService;
    private final SecurityUtils securityUtils;
    private final RedissonClient redissonClient;


    public BrainController(IBrainService brainService, Session agentSession, SecurityUtils securityUtils, IBrainHistoryService brainHistoryService, RedissonClient redissonClient) {
        this.brainHistoryService = brainHistoryService;
        this.securityUtils = securityUtils;
        this.redissonClient = redissonClient;
        // ... 其余不变
```

新增接口方法（放在 `deleteSession` 方法之后）：

```java
    @Operation(summary = "前端工具执行结果回调")
    @PostMapping("tool/callback")
    public R<Void> toolCallback(@RequestBody WebToolCallbackRequest request) {
        return aguiController.handleToolCallback(request, redissonClient);
    }
```

- [ ] **Step 2: Commit**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/controller/BrainController.java
git commit -m "feat: BrainController新增工具回调接口 POST brain/tool/callback"
```

---

### Task 7: 后端 - 更新 WebTool 使用新的 webExecuteTool 签名

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/service/tool/WebTool.java`

- [ ] **Step 1: 更新 WebTool.routeNavigation 方法**

```java
package org.quyq.gwsu.security.brain.service.tool;


import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.agui.web.WebToolType;

import java.util.Map;

/**
 * Web端工具集合
 */
public class WebTool {

    @Tool(description = "控制web界面跳转到指定路由")
    public ToolResultBlock routeNavigation(@ToolParam(name = "path", description = "跳转的路由地址") String path,
                                           ToolEmitter emitter) {
        return WebToolUtils.webExecuteTool(emitter, "routeNavigation", Map.of("path", path), WebToolType.AUTO);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/service/tool/WebTool.java
git commit -m "feat: WebTool.routeNavigation改用新的webExecuteTool签名，直接返回前端执行结果"
```

---

### Task 8: 后端 - 编译验证

- [ ] **Step 1: 编译整个项目验证无错误**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn clean compile -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 修复编译问题（如有）**

如果编译失败，根据错误信息修复后重新编译。

---

### Task 9: 前端 - 创建 WebTool 类型定义

**Files:**
- Create: `web/apps/gwsu-main/src/services/web-tool/types.ts`

- [ ] **Step 1: 创建类型定义文件**

```typescript
/**
 * Web工具类型
 */
export type WebToolType = 'AUTO' | 'INTERACTIVE';

/**
 * CUSTOM 事件 TOOL_EXECUTE 的 value 结构
 */
export interface WebToolExecutePayload {
  /** 工具调用唯一标识 */
  toolCallId: string;
  /** 工具名称 */
  toolName: string;
  /** 工具类型 */
  toolType: WebToolType;
  /** 工具参数 */
  params: Record<string, unknown>;
}

/**
 * 工具执行结果
 */
export interface WebToolResult {
  /** 是否执行成功 */
  success: boolean;
  /** 执行结果描述 */
  result: string;
}

/**
 * 工具执行器接口
 * 每个前端Web工具都需要实现此接口
 */
export interface WebToolExecutor {
  /** 执行工具，返回结果 */
  execute(params: Record<string, unknown>): Promise<WebToolResult>;
}

/**
 * 工具回调请求体（发送给后端）
 */
export interface WebToolCallbackRequest {
  /** 工具调用唯一标识 */
  toolCallId: string;
  /** 是否执行成功 */
  success: boolean;
  /** 执行结果描述 */
  result: string;
}
```

- [ ] **Step 2: Commit**

```bash
git add web/apps/gwsu-main/src/services/web-tool/types.ts
git commit -m "feat: 前端WebTool类型定义"
```

---

### Task 10: 前端 - 创建工具注册表

**Files:**
- Create: `web/apps/gwsu-main/src/services/web-tool/registry.ts`

- [ ] **Step 1: 创建注册表**

```typescript
import type { WebToolExecutor } from './types';

/** 工具注册表 */
const registry = new Map<string, WebToolExecutor>();

/**
 * 注册Web工具执行器
 * @param name 工具名称，需与后端 @Tool 注解的名称一致
 * @param executor 工具执行器
 */
export function registerWebTool(name: string, executor: WebToolExecutor): void {
  registry.set(name, executor);
}

/**
 * 获取已注册的工具执行器
 * @param name 工具名称
 * @returns 工具执行器，未注册返回 undefined
 */
export function getWebTool(name: string): WebToolExecutor | undefined {
  return registry.get(name);
}
```

- [ ] **Step 2: Commit**

```bash
git add web/apps/gwsu-main/src/services/web-tool/registry.ts
git commit -m "feat: 前端WebTool工具注册表"
```

---

### Task 11: 前端 - 创建工具分发器

**Files:**
- Create: `web/apps/gwsu-main/src/services/web-tool/dispatcher.ts`

- [ ] **Step 1: 创建分发器，封装完整的 事件接收→工具执行→回调结果 流程**

```typescript
import { post } from '@gwsu/core';
import { getWebTool } from './registry';
import type { WebToolExecutePayload, WebToolCallbackRequest } from './types';

/**
 * 分发Web工具执行
 * 通用流程：查找执行器 → 执行工具 → 回调结果
 * 只有"执行工具"步骤调用不同的executor，其余均为通用逻辑
 */
export async function dispatchWebTool(payload: WebToolExecutePayload): Promise<void> {
  const { toolCallId, toolName, params } = payload;
  const executor = getWebTool(toolName);

  if (!executor) {
    await callbackToolResult(toolCallId, false, `未知工具: ${toolName}`);
    return;
  }

  try {
    const result = await executor.execute(params);
    await callbackToolResult(toolCallId, result.success, result.result);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    await callbackToolResult(toolCallId, false, `执行异常: ${message}`);
  }
}

/**
 * 回调工具执行结果给后端
 * 通用逻辑，所有工具执行后统一调用
 */
async function callbackToolResult(
  toolCallId: string,
  success: boolean,
  result: string,
): Promise<void> {
  try {
    await post<unknown>('/security/brain/tool/callback', {
      toolCallId,
      success,
      result,
    } satisfies WebToolCallbackRequest);
  } catch (error) {
    console.error('[WebTool] 回调结果失败:', error);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add web/apps/gwsu-main/src/services/web-tool/dispatcher.ts
git commit -m "feat: 前端WebTool分发器，封装事件接收→工具执行→回调结果通用流程"
```

---

### Task 12: 前端 - 创建统一导出文件

**Files:**
- Create: `web/apps/gwsu-main/src/services/web-tool/index.ts`

- [ ] **Step 1: 创建统一导出**

```typescript
export type { WebToolType, WebToolExecutePayload, WebToolResult, WebToolExecutor, WebToolCallbackRequest } from './types';
export { registerWebTool, getWebTool } from './registry';
export { dispatchWebTool } from './dispatcher';
```

- [ ] **Step 2: Commit**

```bash
git add web/apps/gwsu-main/src/services/web-tool/index.ts
git commit -m "feat: 前端WebTool模块统一导出"
```

---

### Task 13: 前端 - 实现 routeNavigation 工具

**Files:**
- Create: `web/apps/gwsu-main/src/services/web-tool/tools/route-navigation.ts`

- [ ] **Step 1: 实现路由跳转工具**

```typescript
import { history } from 'umi';
import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';

/**
 * 路由跳转工具
 * 后端调用 routeNavigation 时，前端执行 history.push 跳转到指定路由
 */
const routeNavigationTool: WebToolExecutor = {
  async execute(params): Promise<WebToolResult> {
    const { path } = params;
    if (typeof path !== 'string' || !path) {
      return { success: false, result: '参数path不能为空' };
    }

    try {
      history.push(path);
      return { success: true, result: `已跳转到: ${path}` };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return { success: false, result: `跳转失败: ${message}` };
    }
  },
};

// 注册工具
registerWebTool('routeNavigation', routeNavigationTool);
```

- [ ] **Step 2: Commit**

```bash
git add web/apps/gwsu-main/src/services/web-tool/tools/route-navigation.ts
git commit -m "feat: 前端实现routeNavigation工具（路由跳转）"
```

---

### Task 14: 前端 - CopilotKitProvider 订阅 CUSTOM 事件

**Files:**
- Modify: `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx`

- [ ] **Step 1: 修改 CopilotKitProvider，订阅 agent 的 CUSTOM 事件并分发到 WebToolDispatcher**

```typescript
import { CopilotKit, useAgent } from '@copilotkit/react-core';
import { useUserStore } from '@gwsu/core';
import type { ReactNode } from 'react';
import { useEffect, useRef } from 'react';
import { dispatchWebTool } from '@/services/web-tool';
import type { WebToolExecutePayload } from '@/services/web-tool';
// 确保 route-navigation 工具被注册
import '@/services/web-tool/tools/route-navigation';

interface GwsuCopilotKitProviderProps {
  children: ReactNode;
}

/**
 * Agent CUSTOM 事件订阅组件
 * 必须在 CopilotKit 内部使用，因为需要 access to agent context
 */
function WebToolEventListener() {
  const { agent } = useAgent({ agentId: 'brain' });
  const subscriptionRef = useRef<ReturnType<typeof agent.subscribe> | null>(null);

  useEffect(() => {
    if (!agent) return;

    // 清理旧的订阅
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
    }

    const subscriber = {
      onCustomEvent: ({ event }: { event: { name: string; value: unknown } }) => {
        if (event.name === 'TOOL_EXECUTE') {
          dispatchWebTool(event.value as WebToolExecutePayload);
        }
      },
    };

    const subscription = agent.subscribe(subscriber);
    subscriptionRef.current = subscription;

    return () => {
      subscription.unsubscribe();
      subscriptionRef.current = null;
    };
  }, [agent]);

  return null;
}

/**
 * CopilotKit Provider 封装
 * 使用 HttpAgent 直接连接到后端 AG-UI 接口
 */
export function GwsuCopilotKitProvider({ children }: GwsuCopilotKitProviderProps) {

  // 动态获取请求头
  const getHeaders = (): Record<string, string> => {
    const tokenInfo = useUserStore.getState().getTokenInfo();
    const headers: Record<string, string> = {};
    if (tokenInfo?.token) {
      headers['Authorization'] = `Bearer ${tokenInfo.token}`;
    }
    return headers;
  };

  return (
    <CopilotKit
      runtimeUrl="/api/security/brain/run/copilotKit"
      headers={getHeaders}
      agent="brain"
      enableInspector={false}
    >
      <WebToolEventListener />
      {children}
    </CopilotKit>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx
git commit -m "feat: CopilotKitProvider订阅CUSTOM事件，分发TOOL_EXECUTE到WebToolDispatcher"
```

---

### Task 15: 前端 - 编译验证

- [ ] **Step 1: 编译前端项目验证无错误**

```bash
cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm build:core && cd apps/gwsu-main && pnpm build
```

Expected: 构建成功，无 TypeScript 错误

- [ ] **Step 2: 修复编译问题（如有）**

如果编译失败，根据错误信息修复后重新编译。

---

## 自审清单

- **Spec 覆盖**：WebToolType ✓ | WebToolStatus ✓ | WebToolTask ✓ | WebToolInfo ✓ | WebToolUtils.webExecuteTool ✓ | AguiController.handleToolCallback ✓ | BrainController 接口路由 ✓ | WebTool 改造 ✓ | 前端类型 ✓ | 注册表 ✓ | 分发器 ✓ | routeNavigation ✓ | CUSTOM 事件监听 ✓
- **Placeholder 扫描**：无 TBD/TODO/待实现
- **类型一致性**：后端 `WebToolInfo` 字段 (toolCallId, toolName, toolType, params) 与前端 `WebToolExecutePayload` 一致 ✓ | 后端 `WebToolCallbackRequest` (toolCallId, success, result) 与前端 `WebToolCallbackRequest` 一致 ✓ | Redis key 前缀 `gwsu:web-tool:` 在 WebToolUtils 和 AguiController 中一致 ✓ | 信号量 key 前缀 `gwsu:web-tool:sem:` 在 WebToolUtils 和 AguiController 中一致 ✓
