# AI 操作模式设计文档

## 概述

为 WebTool 增加 AI 操作模式机制：AI 操作界面前需先进入 AI 模式（经用户审批），操作完成后主动退出。AI 模式下操作区被遮罩锁定，用户可通过终止组件强制回退人类模式。

## 核心流程

```
AI想操作界面 → 调用EnterAiMode → HumanInTheLoop审批
  ├─ 用户批准 → operationMode='ai' → 遮罩锁定操作区 → AI继续操作(逐个审批)
  │              → AI操作完毕 → 调用ExitAiMode → operationMode='human' → 遮罩消失
  └─ 用户拒绝 → 保持human → AI被告知拒绝，不能操作

用户随时点终止 → operationMode='human' → 后续WebTool执行器拒绝
```

## 一、后端改动

### 1.1 WebTool.java 新增两个工具

**EnterAiMode** — 请求进入 AI 操作模式，带 `@HumanInTheLoop` 审批：

```java
@HumanInTheLoop(tip = "智能助手请求控制界面，是否同意？")
@Tool(name = "EnterAiMode", description = """
        请求进入AI操作模式，获取界面控制权。
        调用此工具后，界面将锁定为AI操作模式，用户无法手动操作界面。
        使用场景：当你需要对界面进行操作（点击、输入、选择、滚动、路由跳转）时，必须先调用此工具获取控制权。
        注意：获取页面状态(GetPageState)不需要进入AI操作模式。
        操作完成后必须调用ExitAiMode退出AI操作模式，将控制权交还给用户。""")
public ToolResultBlock enterAiMode(ToolEmitter emitter) throws TimeoutException {
    return webToolUtils.webExecuteTool(emitter, "EnterAiMode", Map.of());
}
```

**ExitAiMode** — 退出 AI 操作模式，无审批：

```java
@Tool(name = "ExitAiMode", description = """
        退出AI操作模式，将界面控制权交还给用户。
        当你完成所有界面操作后，必须调用此工具退出AI操作模式。""")
public ToolResultBlock exitAiMode(ToolEmitter emitter) throws TimeoutException {
    return webToolUtils.webExecuteTool(emitter, "ExitAiMode", Map.of());
}
```

### 1.2 RouteNavigation 移除审批注解

```java
// 移除 @HumanInTheLoop(tip = "是否同意路由跳转？")
@Tool(name = "RouteNavigation", description = "控制web界面跳转到指定路由")
public ToolResultBlock routeNavigation(...) { ... }
```

## 二、前端改动

### 2.1 新增 WebTool 执行器

**`web-tool/tools/enter-ai-mode.ts`**：
- 调用 `useForwardedPropsStore.getState().setOperationMode('ai')`
- 返回 `{ success: true, result: '已进入AI操作模式' }`

**`web-tool/tools/exit-ai-mode.ts`**：
- 调用 `useForwardedPropsStore.getState().setOperationMode('human')`
- 返回 `{ success: true, result: '已退出AI操作模式' }`

### 2.2 dispatcher.ts 增加操作模式守卫

在 `dispatchWebTool` 中，执行器执行前检查 operationMode。定义需要 AI 模式的工具白名单：

```typescript
/** 需要 AI 操作模式的工具列表 */
const AI_MODE_REQUIRED_TOOLS = [
  'RouteNavigation',
  'ClickElement',
  'InputText',
  'SelectOption',
  'ScrollPage',
];

const mode = useForwardedPropsStore.getState().operationMode;
if (mode === 'human' && AI_MODE_REQUIRED_TOOLS.includes(toolName)) {
  await callbackToolResult(toolCallId, false, '当前为人类操作模式，请先进入AI操作模式');
  return;
}
```

### 2.3 AI 模式遮罩组件（AiModeOverlay）

新组件 `components/AiModeOverlay/`：
- 读取 `useForwardedPropsStore` 的 `operationMode`
- `ai` 模式时渲染全屏遮罩
- 显示 Loading 动画 + "智能助手控制中..." 文字
- 样式：
  - `position: fixed`，覆盖整个视口
  - `z-index: 1050`（高于 Ant Design Modal 默认 1000，低于聊天面板）
  - 半透明背景，居中 Loading
  - 使用 `createPortal` 挂载到 body
- 挂载位置：`InterfaceOperation` 组件内引入（仅遮罩操作区+弹框，不遮罩聊天面板）

### 2.4 终止控制组件（AiModeControlBar）

新组件 `components/AIChat/AiModeControlBar/`，类似 `HumanApprovalBar` 风格：
- 读取 `useForwardedPropsStore` 的 `operationMode`
- 仅当 `operationMode === 'ai'` 时渲染
- 位置：聊天面板中，输入框上方（与 `HumanApprovalBar` 并列）
- 样式：
  - 独立卡片样式，醒目的橙色/红色警告风格
  - 左侧：控制图标 + "智能助手正在控制界面"
  - 右侧：醒目的终止按钮，文字"终止控制"
  - 点击终止 → `setOperationMode('human')`
- 排列顺序（从上到下）：AiModeControlBar → HumanApprovalBar → AskUserQuestionBar → 输入框

### 2.5 CopilotKitProvider 注册新工具

```typescript
import '@/services/web-tool/tools/enter-ai-mode';
import '@/services/web-tool/tools/exit-ai-mode';
```

## 三、z-index 层级策略

```
body
├── #root (主应用)
│   ├── header
│   ├── AssistantOperationArea
│   │   └── InterfaceOperation
│   └── ...
├── Ant Design Modal (z: 1000)    ← 被遮罩覆盖
├── AiModeOverlay (z: 1050)       ← 遮罩覆盖操作区+弹框
└── CopilotChatPanel (z: 100000+) ← 始终最上层，不被遮罩覆盖
```

## 四、边界场景处理

| 场景 | 处理方式 |
|------|----------|
| 用户点终止时 AI 正在执行 WebTool | 当前执行不会被打断，后续调用被前端拒绝返回错误，AI 看到 ExitAiMode |
| 页面刷新 | store 默认值 `human`，自动重置。新会话无需处理 |
| AI 进入模式后未退出就结束对话 | 用户可随时点终止回退；新会话默认 human |
| EnterAiMode 被用户拒绝 | operationMode 保持 human，后续操作被守卫拦截 |
| AI 模式下的 HumanApprovalBar | 正常展示，用户仍可在聊天面板内审批具体操作 |
