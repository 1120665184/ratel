# TOOL_EXECUTE 工具执行框架设计

## 概述

实现后端 AG-UI CUSTOM 事件中 `TOOL_EXECUTE` 的前端执行框架，支持后端通知前端执行工具操作并等待结果返回，具备可扩展性。

## 背景

后端通过 `WebToolExecuteHook` 发送 AG-UI CUSTOM 事件，格式为：
```json
{
  "type": "CUSTOM",
  "threadId": "xxx",
  "runId": "xxx",
  "name": "TOOL_EXECUTE",
  "value": {
    "toolCallId": "uuid",
    "toolName": "routeNavigation",
    "toolType": "AUTO",
    "params": { "path": "/sub-system/user" }
  }
}
```

当前前端未处理 CUSTOM 事件，需要实现：接收事件 → 执行工具 → 回调结果。

## 整体架构

```
LLM 调用 WebTool → webExecuteTool() 发 CUSTOM 事件 + Redisson 信号量等待
        ↓ SSE                              ↑ 回调接口
前端收到 CUSTOM 事件 → 执行工具 → POST /tool/callback → 释放信号量
        ↓
webExecuteTool() 获取结果 → 返回 ToolResultBlock → Agent 继续推理
```

## 后端设计

### 1. 数据模型

#### WebToolType 枚举

| 值 | 含义 | 前端行为 |
|---|------|---------|
| `AUTO` | 自动执行 | 直接执行，自动回调结果 |
| `INTERACTIVE` | 需用户交互 | 展示确认UI，用户操作后回调结果 |

#### WebToolInfo record（扩展现有）

```java
public record WebToolInfo(
    String toolCallId,    // UUID，唯一标识一次工具调用
    String toolName,      // 工具名称
    WebToolType toolType, // 工具类型
    Map<String, Object> params  // 工具参数
) {}
```

#### WebToolTask Redis 存储结构

```java
public record WebToolTask(
    String toolCallId,
    String toolName,
    WebToolType toolType,
    Map<String, Object> params,
    WebToolStatus status,  // PENDING / SUCCESS / FAILED / TIMEOUT
    String result          // 执行结果
) {}
```

- Redis key: `gwsu:web-tool:{toolCallId}`
- 信号量 key: `gwsu:web-tool:sem:{toolCallId}`
- TTL: 120秒（与超时时间对齐）

### 2. WebToolUtils.webExecuteTool()

核心方法，封装完整流程，工具类方法一行调用：

```java
public static ToolResultBlock webExecuteTool(
    ToolEmitter emitter, String toolName,
    Map<String, Object> params, WebToolType toolType) {

    // 1. 生成 toolCallId
    String toolCallId = UUID.randomUUID().toString();

    // 2. 发送 CUSTOM 事件给前端（通过 ToolEmitter）
    WebToolInfo info = new WebToolInfo(toolCallId, toolName, toolType, params);
    emitter.emit(ToolResultBlock.text(WEB_TOOL_IDENTIFICATION + gson.toJson(info)));

    // 3. Redis 存储任务信息
    RBucket<WebToolTask> bucket = redissonClient.getBucket(KEY_PREFIX + toolCallId);
    bucket.set(new WebToolTask(toolCallId, toolName, toolType, params, PENDING, null), 120, TimeUnit.SECONDS);

    // 4. Redisson 信号量等待前端结果
    RSemaphore semaphore = redissonClient.getSemaphore(SEMAPHORE_PREFIX + toolCallId);
    semaphore.trySetPermits(0);
    boolean acquired = semaphore.tryAcquire(60, TimeUnit.SECONDS);

    if (!acquired) {
        bucket.set(bucket.get().withStatus(TIMEOUT), 60, TimeUnit.SECONDS);
        return ToolResultBlock.text("工具执行超时: " + toolName);
    }

    // 5. 获取并返回结果
    WebToolTask task = bucket.get();
    if (task.getStatus() == SUCCESS) {
        return ToolResultBlock.text(task.getResult());
    } else {
        return ToolResultBlock.text("工具执行失败: " + task.getResult());
    }
}
```

超时时间默认 60 秒。

### 3. 回调接口

在 `AguiController` 中新增方法，`BrainController` 定义接口路由：

```
POST /api/security/brain/tool/callback
```

请求体：
```json
{
  "toolCallId": "uuid",
  "success": true,
  "result": "已跳转到: /sub-system/user"
}
```

处理逻辑：
1. 根据 toolCallId 从 Redis 获取 WebToolTask
2. 校验 task 存在且状态为 PENDING
3. 更新 task 的 status 和 result
4. 释放对应信号量，唤醒 `webExecuteTool()` 中的等待线程

### 4. WebTool 保持简洁

```java
@Tool(description = "控制web界面跳转到指定路由")
public ToolResultBlock routeNavigation(
    @ToolParam(name = "path", description = "跳转的路由地址") String path,
    ToolEmitter emitter) {
    return WebToolUtils.webExecuteTool(emitter, "routeNavigation",
        Map.of("path", path), WebToolType.AUTO);
}
```

## 前端设计

### 1. 架构

```
CUSTOM 事件 → onCustomEvent → WebToolDispatcher.dispatch(payload)
                                        ↓
                              WebToolRegistry.get(toolName)
                                        ↓
                              XxxTool.execute(params)
                                        ↓
                              POST /tool/callback {toolCallId, success, result}
```

### 2. 核心类型（types.ts）

```typescript
/** 工具类型 */
type WebToolType = 'AUTO' | 'INTERACTIVE';

/** CUSTOM 事件 value 结构 */
interface WebToolExecutePayload {
  toolCallId: string;
  toolName: string;
  toolType: WebToolType;
  params: Record<string, unknown>;
}

/** 工具执行结果 */
interface WebToolResult {
  success: boolean;
  result: string;
}

/** 工具执行器接口 */
interface WebToolExecutor {
  execute(params: Record<string, unknown>): Promise<WebToolResult>;
}
```

### 3. 工具注册表（registry.ts）

```typescript
const registry = new Map<string, WebToolExecutor>();

export function registerWebTool(name: string, executor: WebToolExecutor): void {
  registry.set(name, executor);
}

export function getWebTool(name: string): WebToolExecutor | undefined {
  return registry.get(name);
}
```

### 4. 工具分发器（dispatcher.ts）

```typescript
export async function dispatchWebTool(payload: WebToolExecutePayload): Promise<void> {
  const { toolCallId, toolName, toolType, params } = payload;
  const executor = getWebTool(toolName);

  if (!executor) {
    await callbackToolResult(toolCallId, false, `未知工具: ${toolName}`);
    return;
  }

  try {
    const result = await executor.execute(params);
    await callbackToolResult(toolCallId, result.success, result.result);
  } catch (error) {
    await callbackToolResult(toolCallId, false, `执行异常: ${error}`);
  }
}

async function callbackToolResult(
  toolCallId: string, success: boolean, result: string
): Promise<void> {
  await post('/security/brain/tool/callback', { toolCallId, success, result });
}
```

### 5. CUSTOM 事件监听

在 CopilotKitProvider 组件树中，利用 `agent.subscribe` 的 `onCustomEvent` 监听：

```typescript
const subscriber: AgentSubscriber = {
  onCustomEvent: ({ event }) => {
    if (event.name === 'TOOL_EXECUTE') {
      dispatchWebTool(event.value as WebToolExecutePayload);
    }
  },
};
```

### 6. RouteNavigation 工具实现

```typescript
const routeNavigationTool: WebToolExecutor = {
  async execute(params) {
    const { path } = params;
    try {
      history.push(path as string);
      return { success: true, result: `已跳转到: ${path}` };
    } catch (error) {
      return { success: false, result: `跳转失败: ${error}` };
    }
  },
};

registerWebTool('routeNavigation', routeNavigationTool);
```

### 7. 文件结构

```
web/apps/gwsu-main/src/
├── services/
│   ├── brain.ts                    # 现有，新增回调 API
│   └── web-tool/                   # 新增
│       ├── types.ts                # WebTool 类型定义
│       ├── registry.ts             # 工具注册表
│       ├── dispatcher.ts           # 工具分发器
│       └── tools/                  # 具体工具实现
│           └── route-navigation.ts # 路由跳转工具
└── providers/
    └── CopilotKitProvider.tsx      # 修改，订阅 CUSTOM 事件
```

## 扩展性

新增工具只需两步：

1. **后端**：新建 WebTool 类，调用 `WebToolUtils.webExecuteTool()`
2. **前端**：新建 `tools/xxx.ts`，实现 `WebToolExecutor` 接口，调用 `registerWebTool('xxx', executor)`

无需修改框架代码。

## 关键设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 等待机制 | Redisson 信号量 | 支持分布式，无轮询开销，项目已集成 Redisson |
| 标识符 | WebToolInfo 中新增 toolCallId (UUID) | Hook 中不便获取 toolCallId，自行生成更可控 |
| 工具类型区分 | AUTO / INTERACTIVE 枚举 | 区分自动执行和需用户交互的场景 |
| webExecuteTool 返回 | 直接返回 ToolResultBlock | 工具方法一行调用，无需再从 Redis 获取结果 |
| 超时时间 | 60秒 | 平衡用户等待体验和工具执行可靠性 |
| Redis TTL | 120秒 | 大于超时时间，确保回调时数据仍存在 |
