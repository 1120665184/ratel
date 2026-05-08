# 智能助手人工干预(Human-in-the-Loop)功能设计

## 概述

为智能助手添加人工干预功能。后端工具方法标注 `@HumanInTheLoop` 注解后，Agent 执行到该工具时暂停，通过 AG-UI CUSTOM 事件发送审批请求给前端，前端展示审批卡片让用户确认/拒绝，用户操作后通过复用 `agent/run` 接口将审批结果传回后端，恢复 Agent 继续执行。

## 核心数据流

```
后端Agent执行 → @HumanInTheLoop注解触发 → HumanInTheLoopHook停止Agent
    → AguiAgentAdapter发送AG-UI CUSTOM(HUMAN_APPROVAL)事件 → SSE流结束
    ↓
前端接收CUSTOM事件 → 解析stage/tip/toolInfo/resultInfo
    → 展示嵌入式审批卡片（聊天输入框上方）
    ↓
用户点击同意/拒绝 → 前端发送approval消息 → 复用agent/run接口
    → 后端AguiRequestProcessor识别approval消息 → 从messages中移除
    → 传approvalResult给AguiAgentAdapter → agent.stream()恢复执行
```

## 一、后端改动

### 1.1 AguiMessage 新增 approval role

在请求的 messages 中，`role` 新增 `approval` 值，`content` 为 JSON 格式：

```json
{
  "role": "approval",
  "content": "{\"result\": \"APPROVED\"}"
}
```

```json
{
  "role": "approval",
  "content": "{\"result\": \"REJECTED\", \"rejectReason\": \"路由跳转不安全\"}"
}
```

字段说明：
- `result`：审批结果，`APPROVED`（同意）或 `REJECTED`（拒绝）
- `rejectReason`：拒绝原因，仅在 POST_REASONING 阶段可选填写，POST_ACTING 阶段不需要

### 1.2 AguiRequestProcessor 处理 approval 消息

在 `process()` 方法中，第98行之前插入 approval 消息处理逻辑（在 `hasMemory` 判断之外）：

```java
public ProcessResult process(RunAgentInput input, String headerAgentId, String pathAgentId) {
    String threadId = input.getThreadId();
    String agentId = resolveAgentId(input, headerAgentId, pathAgentId);
    Agent agent = agentResolver.resolveAgent(agentId, threadId);

    // 1. 检查 approval 消息（独立于 hasMemory 逻辑）
    ApprovalResult approvalResult = null;
    AguiMessage approvalMsg = extractApprovalMessage(input);
    if (approvalMsg != null) {
        approvalResult = parseApprovalResult(approvalMsg);
        input = removeApprovalMessage(input);
    }

    // 2. 正常的 hasMemory 逻辑（仅在非审批恢复时执行）
    RunAgentInput effectiveInput = input;
    if (approvalResult == null && agentResolver.hasMemory(threadId)) {
        effectiveInput = extractLatestUserMessage(input);
    }

    // 3. 传给 adapter
    AguiAgentAdapter adapter = new AguiAgentAdapter(agent, config);
    Flux<AguiEvent> events = adapter.run(effectiveInput, approvalResult);

    return new ProcessResult(agent, events);
}
```

关键逻辑：
- approval 消息检测在 `hasMemory` 判断**之前**
- 检测到 approval 消息后从 messages 中**移除**，不进入上下文历史
- 当 `approvalResult != null` 时，跳过 `extractLatestUserMessage`

### 1.3 AguiAgentAdapter 支持 approvalResult

`run()` 方法新增可选参数 `ApprovalResult`：

```java
public Flux<AguiEvent> run(RunAgentInput input, ApprovalResult approvalResult)
```

恢复执行逻辑（使用 `stream` 方法，不用 `call`）：
- **POST_REASONING + 同意**：`agent.stream()` 无参继续，工具正常执行
- **POST_REASONING + 拒绝**：构建取消的 `ToolResultBlock` 消息，`agent.stream(cancelMsg)` 传入
- **POST_ACTING + 同意**：`agent.stream()` 无参继续，进入下一轮推理
- **POST_ACTING + 拒绝**：构建取消消息，`agent.stream(cancelMsg)` 传入，终止本轮

拒绝时构建的 `cancelMsg`：

**POST_REASONING 阶段**（工具未执行，从待审批的 ToolUseBlock 构建）：
```java
Msg cancelResult = Msg.builder()
    .role(MsgRole.TOOL)
    .content(pendingToolUseBlocks.stream()
        .map(t -> ToolResultBlock.of(t.getId(), t.getName(),
            TextBlock.builder().text(
                rejectReason != null
                    ? "操作已拒绝，原因：" + rejectReason
                    : "操作已拒绝"
            ).build()))
        .toArray(ToolResultBlock[]::new))
    .build();
```

**POST_ACTING 阶段**（工具已执行完成，拒绝意味着不继续下一轮推理，构建一条用户拒绝继续的消息）：
```java
Msg cancelMsg = Msg.builder()
    .role(MsgRole.USER)
    .content(TextBlock.builder().text("用户拒绝继续，终止本轮操作").build())
    .build();
```

### 1.4 新增审批状态查询接口

```
GET /brain/approval/status/{threadId}
```

后端从 Session 加载 Agent，检查是否处于 STOP_REQUESTED 状态（有待审批的 ToolUseBlock），返回：

```json
{
  "pending": true,
  "stage": "POST_REASONING",
  "reasoningStageInfo": [{ "tip": "是否同意路由跳转？", "toolInfo": {...} }],
  "actingStageInfo": null
}
```

用于页面刷新后恢复审批卡片。

### 1.5 新增 ApprovalResult 类

```java
public record ApprovalResult(
    String result,          // "APPROVED" 或 "REJECTED"
    String rejectReason     // 可选，拒绝原因
) {}
```

## 二、前端改动

### 2.1 新增审批类型定义

新建 `services/human-approval/types.ts`：

```ts
/** 审批阶段 */
export type ApprovalStage = 'POST_REASONING' | 'POST_ACTING';

/** 审批结果 */
export type ApprovalResult = 'APPROVED' | 'REJECTED';

/** CUSTOM 事件 HUMAN_APPROVAL 的 value 结构（对应后端 HumanApprovalInfo） */
export interface HumanApprovalPayload {
  stage: ApprovalStage;
  reasoningStageInfo: ReasoningStageInfo[] | null;
  actingStageInfo: ActingStageInfo | null;
}

/** POST_REASONING 阶段审批信息 */
export interface ReasoningStageInfo {
  tip: string;
  toolInfo: {
    type: 'tool_use';
    id: string;
    name: string;
    input: Record<string, unknown>;
    content: string;
  };
}

/** POST_ACTING 阶段审批信息 */
export interface ActingStageInfo {
  tip: string;
  resultInfo: {
    type: 'tool_result';
    id: string;
    name: string;
    output: { type: string; text: string }[];
  };
}

/** 审批回调请求体（发送给后端） */
export interface ApprovalCallbackPayload {
  result: ApprovalResult;
  rejectReason?: string;
}
```

### 2.2 审批状态管理

新建 `services/human-approval/store.ts`：

- `pendingApproval`：当前待审批事件（同一时刻只有一个）
- `dispatchHumanApproval(payload)`：接收审批事件，设置状态
- `clearHumanApproval()`：清除审批状态
- `onHumanApproval(listener)`：注册审批事件监听（供UI组件使用）

### 2.3 CopilotKitProvider 扩展

在 `WebToolEventListener` 中扩展 `onCustomEvent` 处理：

```tsx
onCustomEvent: ({ event }) => {
  if (event.name === 'TOOL_EXECUTE') {
    dispatchWebTool(event.value as WebToolExecutePayload);
  } else if (event.name === 'HUMAN_APPROVAL') {
    dispatchHumanApproval(event.value as HumanApprovalPayload);
  }
}
```

### 2.4 嵌入式审批卡片组件

新建 `components/AIChat/HumanApprovalBar.tsx` + `HumanApprovalBar.module.less`

**位置**：聊天输入框上方、消息列表下方，固定定位在智能助手面板内。

**UI结构**：
- 左侧：警告图标 + tip 提示文案
- 中部：工具名称和参数/结果摘要（可折叠展开）
- 右侧：同意/拒绝按钮
- POST_REASONING 阶段拒绝时，展开一个拒绝原因输入框

**交互**：
- 点击"同意"：调用 `submitApproval('APPROVED')`
- 点击"拒绝"：
  - POST_REASONING 阶段：展开拒绝原因输入框（可选填写），确认后调用 `submitApproval('REJECTED', reason)`
  - POST_ACTING 阶段：直接调用 `submitApproval('REJECTED')`

### 2.5 审批结果提交逻辑

`submitApproval` 的实现（伪代码，具体 API 名称在实施时根据 CopilotKit SDK 确认）：

```ts
async function submitApproval(result: ApprovalResult, rejectReason?: string) {
  clearHumanApproval();

  // 构造 approval 消息
  const approvalContent = result === 'REJECTED' && rejectReason
    ? { result, rejectReason }
    : { result };

  // 将 approval 消息追加到对话中，然后触发 agent/run 请求
  // 具体实现方式：使用 useCopilotChat 的 appendMessage + sendMessage，
  // 或使用 useAgent 的 agent.addMessage + copilotkit.runAgent
  // approval 消息在前端不渲染到聊天界面，后端处理后会从 messages 中移除
}
```

关键点：
- approval 消息 `role: 'approval'`，后端识别后从 messages 中移除
- 前端不应将 approval 消息渲染到聊天界面
- 利用 CopilotKit 机制重新发起 `agent/run` 请求，后端通过 threadId 恢复 Agent 实例
- 具体的 CopilotKit API 调用方式在实施阶段确认

### 2.6 历史会话恢复审批

在 `CopilotChatPanel.tsx` 的 `handleLoadSession` 中增加逻辑：

1. 加载历史消息后，检查最后一条 assistant 消息是否包含未完成的 ToolUseBlock（无对应 ToolResultBlock）
2. 如果有，调用 `GET /brain/approval/status/{threadId}` 获取审批详情
3. 如果 `pending: true`，恢复审批卡片展示

新增 API：
```ts
// services/brain.ts
export async function getApprovalStatus(threadId: string): Promise<HumanApprovalPayload & { pending: boolean }> {
  const response = await get<HumanApprovalPayload & { pending: boolean }>(
    `/security/brain/approval/status/${threadId}`
  );
  return response.data;
}
```

## 三、文件变更清单

### 后端

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `AguiRequestProcessor.java` | 修改 | 新增 approval 消息提取、解析、移除逻辑 |
| `AguiAgentAdapter.java` | 修改 | `run()` 方法支持 ApprovalResult 参数，恢复执行逻辑 |
| `ApprovalResult.java` | 新增 | 审批结果 record 类 |
| `BrainController.java` | 修改 | 新增审批状态查询接口 |
| `AguiMessage.java` | 无改动 | role 字段已是 String，直接支持 "approval" |

### 前端

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `services/human-approval/types.ts` | 新增 | 审批类型定义 |
| `services/human-approval/store.ts` | 新增 | 审批状态管理 |
| `services/human-approval/index.ts` | 新增 | 导出 |
| `services/brain.ts` | 修改 | 新增 getApprovalStatus API |
| `providers/CopilotKitProvider.tsx` | 修改 | 扩展 onCustomEvent 监听 HUMAN_APPROVAL |
| `components/AIChat/HumanApprovalBar.tsx` | 新增 | 嵌入式审批卡片组件 |
| `components/AIChat/HumanApprovalBar.module.less` | 新增 | 审批卡片样式 |
| `components/AIChat/CopilotChatPanel.tsx` | 修改 | 集成 HumanApprovalBar + 历史恢复审批逻辑 |

## 四、审批结果对 Agent 的影响

| 场景 | 阶段 | 用户操作 | Agent行为 |
|------|------|----------|-----------|
| 工具执行前 | POST_REASONING | 同意 | agent.stream() 继续执行工具 |
| 工具执行前 | POST_REASONING | 拒绝 | agent.stream(cancelMsg) 工具不执行，返回拒绝信息 |
| 工具执行后 | POST_ACTING | 同意 | agent.stream() 进入下一轮推理 |
| 工具执行后 | POST_ACTING | 拒绝 | agent.stream(cancelMsg) 终止本轮，返回拒绝信息 |
