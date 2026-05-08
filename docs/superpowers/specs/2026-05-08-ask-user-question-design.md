# 智能助手 AskUserQuestion 功能设计

## 概述

为智能助手添加 AskUserQuestion 弹框功能。当 Agent 调用 `AskUserQuestion` 工具时，前端监听 AG-UI 协议的 `ToolCallStartEvent` 弹出选择框，用户作答后以 `role: 'tool'` 标准消息提交结果并恢复 Agent 执行。

## 核心数据流

```
后端Agent调用AskUserQuestion工具 → ToolSuspendException暂停
    → AguiAgentAdapter发送ToolCallStart/ToolCallArgs事件 → SSE流结束
    ↓
前端AgentSubscriber.onToolCallStartEvent → 检测name==='AskUserQuestion'
    → 缓冲ToolCallArgsEvent收集完整参数 → dispatchAskUserQuestion(payload)
    → AskUserQuestionBar弹出选择框，ChatInput禁用
    ↓
用户选择/填写答案 → 构造role:'tool'消息 → agent.addMessage + agent.runAgent()
    → 后端识别tool消息 → 恢复Agent执行
    → 前端清除弹框状态，恢复输入框
```

## 一、触发机制

### 1.1 AG-UI 事件监听

在 `CopilotKitProvider.tsx` 的 `WebToolEventListener` 中，通过 `agent.subscribe` 监听以下事件：

- **`onToolCallStartEvent`**：检测工具名称是否为 `AskUserQuestion`，若是则开始缓冲参数
- **`onToolCallArgsEvent`**：收集工具参数的 delta 片段，拼接完整参数
- **`onToolCallEndEvent`**：参数收集完毕，解析 questions 并 `dispatchAskUserQuestion`

> AG-UI 协议中 `ToolCallStart` 只包含 `toolCallId` 和 `name`，参数通过 `ToolCallArgsEvent` 以 delta 方式增量传输，`ToolCallEnd` 标志参数传输完成。因此需要用 ref 缓冲 args，在 `ToolCallEnd` 时一次性解析并 dispatch。

### 1.2 历史会话恢复

加载历史会话后，检查最新一条 assistant 消息是否包含 `AskUserQuestion` 的 ToolUseBlock（无对应 ToolResultBlock），如果有则通过审批状态查询接口 `GET /brain/approval/status/{threadId}` 恢复弹框。

## 二、类型定义

新建 `services/ask-user-question/types.ts`：

```ts
/** 问题选项 */
export interface QuestionOption {
  label: string;
  description: string;
}

/** 问题参数（对应后端 QuestionParam） */
export interface QuestionParam {
  question: string;
  header: string;
  options: QuestionOption[];
  multiSelect: boolean;
}

/** AskUserQuestion 事件载荷 */
export interface AskUserQuestionPayload {
  toolCallId: string;
  questions: QuestionParam[];
}

/** 用户作答结果 */
export interface AskUserQuestionAnswer {
  /** key=question, value=选中的label（多选时逗号分隔） */
  answers: Record<string, string>;
  /** 可选的备注信息 */
  annotations: Record<string, { preview?: string; notes?: string }>;
}
```

## 三、状态管理

新建 `services/ask-user-question/store.ts`，参照 `human-approval/store.ts` 模式：

- `dispatchAskUserQuestion(payload)` — 设置待回答问题，通知所有监听器
- `clearAskUserQuestion()` — 清除状态，通知所有监听器（传 null）
- `onAskUserQuestion(listener)` — 注册监听器，返回取消监听函数
- `getPendingAskUserQuestion()` — 获取当前待回答问题

## 四、AskUserQuestionBar 组件

### 4.1 位置

与 `HumanApprovalBar` 平级，放在聊天输入框上方（`CopilotChatPanel` 中 `HumanApprovalBar` 下方）。

### 4.2 UI 结构

每个问题一个卡片区域：

```
┌──────────────────────────────────────┐
│ ❓ [header标签]                       │
│                                       │
│ question 问题文本？                    │
│                                       │
│ ○ 选项A (Recommended)                 │
│   选项A的描述说明                      │
│                                       │
│ ○ 选项B                               │
│   选项B的描述说明                      │
│                                       │
│ ○ Other                               │
│ ┌─────────────────────────────────┐   │
│ │ 自由输入...                       │   │
│ └─────────────────────────────────┘   │
│                                       │
│ （multiSelect时 ○ 变 □）              │
└──────────────────────────────────────┘

        [提交答案]
```

关键交互：
- `multiSelect: false` → Radio 单选
- `multiSelect: true` → Checkbox 多选
- 每个 "Other" 选项带自由输入框
- 底部提交按钮，提交后禁用防重复点击

### 4.3 提交逻辑

```ts
const submitAnswer = async (answer: AskUserQuestionAnswer) => {
  if (!payload) return;
  setSubmitting(true);
  try {
    const toolMsgId = crypto.randomUUID();
    agent.addMessage({
      id: toolMsgId,
      role: 'tool',
      content: JSON.stringify(answer),
      toolCallId: payload.toolCallId,
    } as any);

    await agent.runAgent();
    clearAskUserQuestion();
  } catch (error) {
    console.error('[AskUserQuestion] 提交答案失败:', error);
  } finally {
    setSubmitting(false);
  }
};
```

关键点：
- `role: 'tool'` 是标准消息格式，后端会正常处理，不需要前端手动从 messages 中移除
- 提交后清除弹框状态，恢复输入框可用

## 五、输入框互斥控制

当 `HumanApprovalBar` 或 `AskUserQuestionBar` 任一弹框显示时，聊天输入框必须禁用，防止用户在弹框未处理时发送消息。

### 5.1 互斥状态

在 `CopilotChatPanel` 中通过统一状态管理：

```ts
const [hasApproval, setHasApproval] = useState(false);
const [hasAskQuestion, setHasAskQuestion] = useState(false);
const isInteractionActive = hasApproval || hasAskQuestion;
```

两个弹框组件各自通过回调通知父组件当前是否有弹框处于活动状态。

### 5.2 禁用输入框

CopilotChat 的输入框是内部组件，不能直接传 prop。通过以下方式禁用：

- 当 `isInteractionActive` 为 true 时，在 `copilot-override.module.less` 中添加 CSS 样式覆盖，隐藏或禁用 CopilotChat 内部的输入区域
- 或通过 `isLoading` 属性间接控制（CopilotChat 的 `isLoading` 为 true 时输入框禁用）

## 六、CopilotKitProvider 改动

在 `WebToolEventListener` 的 `agent.subscribe` 中扩展：

```ts
const subscriber: AgentSubscriber = {
  onCustomEvent: ({ event }) => {
    // 现有逻辑：TOOL_EXECUTE / HUMAN_APPROVAL
  },
  onToolCallStartEvent: ({ event }) => {
    if (event.name === 'AskUserQuestion') {
      // 开始缓冲参数
      askUserQuestionArgsRef.current = { toolCallId: event.toolCallId, argsDelta: '' };
    }
  },
  onToolCallArgsEvent: ({ event }) => {
    const ref = askUserQuestionArgsRef.current;
    if (ref) {
      ref.argsDelta += event.delta;
    }
  },
  onToolCallEndEvent: ({ event }) => {
    const ref = askUserQuestionArgsRef.current;
    if (ref) {
      const params = JSON.parse(ref.argsDelta);
      dispatchAskUserQuestion({
        toolCallId: ref.toolCallId,
        questions: params.questions,
      });
      askUserQuestionArgsRef.current = null;
    }
  },
};
```

## 七、文件变更清单

### 前端新增

| 文件 | 说明 |
|------|------|
| `services/ask-user-question/types.ts` | 类型定义 |
| `services/ask-user-question/store.ts` | 状态管理（事件分发、监听） |
| `services/ask-user-question/index.ts` | 导出 |
| `components/AIChat/AskUserQuestionBar.tsx` | 选择框组件 |
| `components/AIChat/AskUserQuestionBar.module.less` | 选择框样式 |

### 前端修改

| 文件 | 说明 |
|------|------|
| `providers/CopilotKitProvider.tsx` | 增加 onToolCallStartEvent/onToolCallArgsEvent/onToolCallEndEvent 监听 |
| `components/AIChat/CopilotChatPanel.tsx` | 集成 AskUserQuestionBar + 互斥控制状态 |
| `components/AIChat/copilot-override.module.less` | 互斥时禁用输入框的 CSS 覆盖 |

### 后端

无需改动。`AskUserQuestionTool` 已实现，`ToolSuspendException` 暂停和恢复机制已就绪。
