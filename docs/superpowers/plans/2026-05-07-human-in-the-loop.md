# 智能助手人工干预(HITL)实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为智能助手添加人工干预功能，后端@HumanInTheLoop注解触发暂停后，前端接收CUSTOM事件展示审批卡片，用户审批后复用agent/run接口恢复Agent执行。

**Architecture:** 后端在AguiRequestProcessor中识别approval消息，解析后传给AguiAgentAdapter恢复Agent执行(使用stream方法)。前端通过CopilotKit的agent.subscribe监听HUMAN_APPROVAL自定义事件，展示嵌入式审批卡片，审批后发送approval消息触发新的agent/run请求。

**Tech Stack:** Java 25 / Spring Boot 4.0.3 / AgentScope SDK / React 18 / TypeScript / CopilotKit / Ant Design 6

---

## 文件结构

### 后端 — 新增文件
| 文件 | 职责 |
|------|------|
| `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/loop/domain/ApprovalResult.java` | 审批结果 record 类 |

### 后端 — 修改文件
| 文件 | 职责 |
|------|------|
| `common/common-ai/src/main/java/io/agentscope/core/agui/processor/AguiRequestProcessor.java` | 新增 approval 消息提取、解析、移除逻辑 |
| `common/common-ai/src/main/java/io/agentscope/core/agui/adapter/AguiAgentAdapter.java` | run() 支持 ApprovalResult 参数，审批恢复执行逻辑 |
| `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/controller/BrainController.java` | 新增审批状态查询接口 |

### 前端 — 新增文件
| 文件 | 职责 |
|------|------|
| `web/apps/gwsu-main/src/services/human-approval/types.ts` | 审批类型定义 |
| `web/apps/gwsu-main/src/services/human-approval/store.ts` | 审批状态管理（事件分发、监听） |
| `web/apps/gwsu-main/src/services/human-approval/index.ts` | 导出 |
| `web/apps/gwsu-main/src/components/AIChat/HumanApprovalBar.tsx` | 嵌入式审批卡片组件 |
| `web/apps/gwsu-main/src/components/AIChat/HumanApprovalBar.module.less` | 审批卡片样式 |

### 前端 — 修改文件
| 文件 | 职责 |
|------|------|
| `web/apps/gwsu-main/src/services/brain.ts` | 新增 getApprovalStatus API |
| `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx` | 扩展 onCustomEvent 监听 HUMAN_APPROVAL |
| `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx` | 集成 HumanApprovalBar + 历史恢复审批 |

---

## Task 1: 后端 — 新增 ApprovalResult 类

**Files:**
- Create: `common/common-ai/src/main/java/org/quyq/gwsu/common/ai/loop/domain/ApprovalResult.java`

- [ ] **Step 1: 创建 ApprovalResult record 类**

```java
package org.quyq.gwsu.common.ai.loop.domain;

/**
 * 人工审批结果
 *
 * @param result       审批结果：APPROVED（同意）或 REJECTED（拒绝）
 * @param rejectReason 拒绝原因，可选，仅在 POST_REASONING 阶段填写
 */
public record ApprovalResult(
        String result,
        String rejectReason
) {

    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    /**
     * 判断是否为同意
     */
    public boolean isApproved() {
        return APPROVED.equals(result);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn compile -pl common/common-ai -am -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add common/common-ai/src/main/java/org/quyq/gwsu/common/ai/loop/domain/ApprovalResult.java
git commit -m "feat(hitl): 新增 ApprovalResult 审批结果 record 类"
```

---

## Task 2: 后端 — AguiRequestProcessor 支持 approval 消息处理

**Files:**
- Modify: `common/common-ai/src/main/java/io/agentscope/core/agui/processor/AguiRequestProcessor.java`

- [ ] **Step 1: 在 AguiRequestProcessor 中添加 approval 消息处理方法**

在 `AguiRequestProcessor.java` 中添加以下三个私有方法（放在 `extractLatestUserMessage` 方法之后）：

```java
/**
 * 从输入消息中提取 approval 消息。
 * approval 消息的 role 为 "approval"，content 为 JSON 格式的审批结果。
 *
 * @param input 原始输入
 * @return approval 消息，如果不存在返回 null
 */
private AguiMessage extractApprovalMessage(RunAgentInput input) {
    List<AguiMessage> messages = input.getMessages();
    if (messages == null || messages.isEmpty()) {
        return null;
    }
    for (int i = messages.size() - 1; i >= 0; i--) {
        AguiMessage msg = messages.get(i);
        if ("approval".equalsIgnoreCase(msg.getRole())) {
            return msg;
        }
    }
    return null;
}

/**
 * 解析 approval 消息的 content，构建 ApprovalResult。
 *
 * @param approvalMsg approval 消息
 * @return 审批结果
 */
private ApprovalResult parseApprovalResult(AguiMessage approvalMsg) {
    String content = approvalMsg.getContent();
    if (content == null || content.isEmpty()) {
        logger.warn("Approval message has empty content, defaulting to REJECTED");
        return new ApprovalResult(ApprovalResult.REJECTED, null);
    }
    try {
        io.agentscope.core.util.JsonUtils.JsonCodec codec = io.agentscope.core.util.JsonUtils.getJsonCodec();
        java.util.Map<String, Object> map = codec.fromJson(content, java.util.Map.class);
        String result = (String) map.get("result");
        String rejectReason = (String) map.get("rejectReason");
        return new ApprovalResult(result, rejectReason);
    } catch (Exception e) {
        logger.error("Failed to parse approval message content: {}", content, e);
        return new ApprovalResult(ApprovalResult.REJECTED, null);
    }
}

/**
 * 从输入消息中移除 approval 消息，返回新的 RunAgentInput。
 * approval 消息不进入上下文历史，处理完后必须移除。
 *
 * @param input 原始输入
 * @return 移除 approval 消息后的新输入
 */
private RunAgentInput removeApprovalMessage(RunAgentInput input) {
    List<AguiMessage> messages = input.getMessages();
    List<AguiMessage> filtered = messages.stream()
            .filter(msg -> !"approval".equalsIgnoreCase(msg.getRole()))
            .toList();
    return RunAgentInput.builder()
            .threadId(input.getThreadId())
            .runId(input.getRunId())
            .messages(filtered)
            .tools(input.getTools())
            .context(input.getContext())
            .forwardedProps(input.getForwardedProps())
            .build();
}
```

- [ ] **Step 2: 修改 process() 方法，在 hasMemory 判断之前插入 approval 处理**

将 `process()` 方法修改为：

```java
public ProcessResult process(RunAgentInput input, String headerAgentId, String pathAgentId) {
    String threadId = input.getThreadId();

    // Resolve agent ID
    String agentId = resolveAgentId(input, headerAgentId, pathAgentId);

    // Resolve agent
    Agent agent = agentResolver.resolveAgent(agentId, threadId);

    // 1. 检查 approval 消息（独立于 hasMemory 逻辑，在之前处理）
    ApprovalResult approvalResult = null;
    AguiMessage approvalMsg = extractApprovalMessage(input);
    if (approvalMsg != null) {
        approvalResult = parseApprovalResult(approvalMsg);
        input = removeApprovalMessage(input);
        logger.debug("Approval message found for thread {}: result={}", threadId, approvalResult.result());
    }

    // 2. 正常的 hasMemory 逻辑（仅在非审批恢复时执行）
    RunAgentInput effectiveInput = input;
    if (approvalResult == null && agentResolver.hasMemory(threadId)) {
        logger.debug(
                "Using server-side memory for thread {}, extracting latest user message",
                threadId);
        effectiveInput = extractLatestUserMessage(input);
    }

    // 3. 传给 adapter（包含审批结果）
    AguiAgentAdapter adapter = new AguiAgentAdapter(agent, config);
    Flux<AguiEvent> events = adapter.run(effectiveInput, approvalResult);

    return new ProcessResult(agent, events);
}
```

- [ ] **Step 3: 添加必要的 import**

在文件顶部 import 区域添加：

```java
import org.quyq.gwsu.common.ai.loop.domain.ApprovalResult;
```

- [ ] **Step 4: 验证编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn compile -pl common/common-ai -am -DskipTests -q`
Expected: BUILD SUCCESS（注意：此时 AguiAgentAdapter.run() 还不支持第二个参数，编译会失败，Task 3 修复）

> 注意：Task 2 和 Task 3 需要一起编译才能通过，可以合并验证。

- [ ] **Step 5: 提交**

```bash
git add common/common-ai/src/main/java/io/agentscope/core/agui/processor/AguiRequestProcessor.java
git commit -m "feat(hitl): AguiRequestProcessor 支持 approval 消息提取、解析和移除"
```

---

## Task 3: 后端 — AguiAgentAdapter 支持审批恢复执行

**Files:**
- Modify: `common/common-ai/src/main/java/io/agentscope/core/agui/adapter/AguiAgentAdapter.java`

- [ ] **Step 1: 修改 run() 方法签名，支持 ApprovalResult 参数**

将现有的 `run(RunAgentInput input)` 方法改为 `run(RunAgentInput input, ApprovalResult approvalResult)`，并在方法内部增加审批恢复逻辑。

替换整个 `run` 方法为：

```java
/**
 * Run the agent with AG-UI protocol input.
 *
 * <p>This method converts the input messages, invokes the agent's streaming API,
 * and emits AG-UI protocol events.
 *
 * <p>When an {@link ApprovalResult} is provided, the agent is resumed from a
 * human-in-the-loop pause rather than starting a new conversation.
 *
 * @param input The AG-UI run input
 * @param approvalResult The approval result for resuming from HITL pause (may be null)
 * @return A Flux of AG-UI events
 */
public Flux<AguiEvent> run(RunAgentInput input, ApprovalResult approvalResult) {
    String threadId = input.getThreadId();
    String runId = input.getRunId();

    // Create stream options - use incremental mode for true streaming
    StreamOptions options =
            StreamOptions.builder().eventTypes(EventType.ALL).incremental(true).build();

    // Track state for event conversion
    EventConversionState state = new EventConversionState(threadId, runId);

    // Determine the event stream based on whether this is an approval resume
    Flux<Event> agentEvents;
    if (approvalResult != null) {
        agentEvents = resumeFromApproval(approvalResult, options);
    } else {
        // Normal flow: convert AG-UI messages to AgentScope messages
        List<Msg> msgs = messageConverter.toMsgList(input.getMessages());
        agentEvents = agent.stream(msgs, options);
    }

    return Flux.concat(
                    // Emit RUN_STARTED
                    Flux.just(new AguiEvent.RunStarted(threadId, runId)),
                    // Stream agent events and convert to AG-UI events
                    agentEvents.concatMapIterable(event -> convertEvent(event, state)),
                    // Emit any pending end events and RUN_FINISHED
                    Flux.defer(() -> finishRun(state)))
            .onErrorResume(
                    error -> {
                        // On error, emit RawEvent with error info followed by RunFinished
                        String errorMessage =
                                error.getMessage() != null
                                        ? error.getMessage()
                                        : error.getClass().getSimpleName();
                        return Flux.just(
                                new AguiEvent.Raw(
                                        threadId, runId, Map.of("error", errorMessage)),
                                new AguiEvent.RunFinished(threadId, runId));
                    });
}
```

- [ ] **Step 2: 添加 resumeFromApproval 私有方法**

在 `run()` 方法之后、`convertEvent()` 方法之前添加：

```java
/**
 * Resume agent execution from a human-in-the-loop pause.
 *
 * <p>Based on the approval result:
 * <ul>
 *   <li>APPROVED: Continue execution (stream with no message)</li>
 *   <li>REJECTED + POST_REASONING: Send cancel ToolResultBlock to skip tool execution</li>
 *   <li>REJECTED + POST_ACTING: Send user message to stop further reasoning</li>
 * </ul>
 *
 * @param approvalResult The approval result
 * @param options Stream options
 * @return Flux of agent events
 */
private Flux<Event> resumeFromApproval(ApprovalResult approvalResult, StreamOptions options) {
    if (approvalResult.isApproved()) {
        // User approved: continue execution without additional message
        logger.debug("Resuming agent after approval");
        return agent.stream(options);
    }

    // User rejected: build cancel message based on pause stage
    // Determine the pause stage from the agent's last generate reason
    Msg lastResult = agent.getLastResult();
    GenerateReason reason = lastResult != null ? lastResult.getGenerateReason() : null;

    if (GenerateReason.REASONING_STOP_REQUESTED == reason) {
        // POST_REASONING: tool not yet executed, build cancel ToolResultBlock
        List<ToolUseBlock> pendingTools = lastResult.getContentBlocks(ToolUseBlock.class);
        String cancelText = approvalResult.rejectReason() != null
                ? "操作已拒绝，原因：" + approvalResult.rejectReason()
                : "操作已拒绝";

        Msg cancelMsg = Msg.builder()
                .role(MsgRole.TOOL)
                .content(pendingTools.stream()
                        .map(t -> ToolResultBlock.of(t.getId(), t.getName(),
                                TextBlock.builder().text(cancelText).build()))
                        .toArray(ToolResultBlock[]::new))
                .build();

        logger.debug("Resuming agent after rejection (POST_REASONING): {}", cancelText);
        return agent.stream(cancelMsg, options);
    } else if (GenerateReason.ACTING_STOP_REQUESTED == reason) {
        // POST_ACTING: tool already executed, reject means stop further reasoning
        Msg cancelMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("用户拒绝继续，终止本轮操作").build())
                .build();

        logger.debug("Resuming agent after rejection (POST_ACTING)");
        return agent.stream(cancelMsg, options);
    } else {
        // Fallback: cannot determine stage, just continue
        logger.warn("Cannot determine HITL pause stage, continuing execution");
        return agent.stream(options);
    }
}
```

- [ ] **Step 3: 添加必要的 import**

在文件顶部 import 区域添加：

```java
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.MsgRole;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalResult;
```

- [ ] **Step 4: 验证 Task 2 + Task 3 编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn compile -pl common/common-ai -am -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add common/common-ai/src/main/java/io/agentscope/core/agui/adapter/AguiAgentAdapter.java
git commit -m "feat(hitl): AguiAgentAdapter 支持审批恢复执行，同意继续/拒绝取消"
```

---

## Task 4: 后端 — BrainController 新增审批状态查询接口

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/controller/BrainController.java`

- [ ] **Step 1: 在 BrainController 中添加审批状态查询接口**

在 `toolCallback` 方法之后添加：

```java
@Operation(summary = "查询会话审批状态")
@GetMapping("approval/status/{threadId}")
public R<HumanApprovalInfo> getApprovalStatus(@PathVariable String threadId) {
    return aguiController.handleApprovalStatus(threadId);
}
```

添加 import：

```java
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
```

- [ ] **Step 2: 在 AguiController 中添加 handleApprovalStatus 方法**

在 `AguiController.java` 中添加：

```java
/**
 * 处理审批状态查询
 * 从 Session 中加载 Agent，检查是否处于 STOP_REQUESTED 状态
 *
 * @param threadId 会话ID
 * @return 审批状态信息
 */
public R<HumanApprovalInfo> handleApprovalStatus(String threadId) {
    if (agentSession == null) {
        return R.ok(new HumanApprovalInfo(null, null, null));
    }

    try {
        String userId = getCurrUserId();
        Agent agent = agentSession.load(CommonSessionKey.of(threadId, userId));
        if (agent == null) {
            return R.ok(new HumanApprovalInfo(null, null, null));
        }

        Msg lastResult = agent.getLastResult();
        if (lastResult == null) {
            return R.ok(new HumanApprovalInfo(null, null, null));
        }

        GenerateReason reason = lastResult.getGenerateReason();
        if (GenerateReason.REASONING_STOP_REQUESTED == reason) {
            List<ToolUseBlock> contentBlocks = lastResult.getContentBlocks(ToolUseBlock.class);
            List<ApprovalTips> approvalToolNames = Optional.ofNullable(
                    (List<ApprovalTips>) lastResult.getMetadata().get(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)
            ).orElse(Collections.emptyList());

            List<HumanApprovalInfo.ReasoningStateInfo> reasoningInfo = approvalToolNames.stream()
                    .map(t -> {
                        Optional<ToolUseBlock> toolUseBlock = contentBlocks.stream()
                                .filter(c -> c.getName().equals(t.toolName()))
                                .findFirst();
                        return new HumanApprovalInfo.ReasoningStateInfo(t.tip(), toolUseBlock.orElse(null));
                    })
                    .toList();

            return R.ok(new HumanApprovalInfo(ApprovalStage.POST_REASONING, reasoningInfo, null));
        } else if (GenerateReason.ACTING_STOP_REQUESTED == reason) {
            List<ToolResultBlock> contentBlocks = lastResult.getContentBlocks(ToolResultBlock.class);
            List<ApprovalTips> approvalToolNames = Optional.ofNullable(
                    (List<ApprovalTips>) lastResult.getMetadata().get(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY)
            ).orElse(Collections.emptyList());

            return approvalToolNames.stream()
                    .map(t -> {
                        Optional<ToolResultBlock> resultBlock = contentBlocks.stream()
                                .filter(c -> c.getName().equals(t.toolName()))
                                .findFirst();
                        return new HumanApprovalInfo.ActingStageInfo(t.tip(), resultBlock.orElse(null));
                    })
                    .findFirst()
                    .map(stageInfo -> R.ok(new HumanApprovalInfo(ApprovalStage.POST_ACTING, null, stageInfo)))
                    .orElse(R.ok(new HumanApprovalInfo(null, null, null)));
        }

        return R.ok(new HumanApprovalInfo(null, null, null));
    } catch (Exception e) {
        log.error("Failed to query approval status for thread {}: {}", threadId, e.getMessage());
        return R.ok(new HumanApprovalInfo(null, null, null));
    }
}
```

添加必要的 import 到 `AguiController.java`：

```java
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.quyq.gwsu.common.ai.session.CommonSessionKey;
import org.quyq.gwsu.common.core.domain.R;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
```

- [ ] **Step 3: 验证编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn compile -pl business/business-security/business-security-server -am -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/controller/BrainController.java
git add common/common-ai/src/main/java/org/quyq/gwsu/common/ai/agui/AguiController.java
git commit -m "feat(hitl): 新增审批状态查询接口 GET /brain/approval/status/{threadId}"
```

---

## Task 5: 前端 — 新增审批类型定义和状态管理

**Files:**
- Create: `web/apps/gwsu-main/src/services/human-approval/types.ts`
- Create: `web/apps/gwsu-main/src/services/human-approval/store.ts`
- Create: `web/apps/gwsu-main/src/services/human-approval/index.ts`

- [ ] **Step 1: 创建类型定义文件 types.ts**

```ts
/**
 * 审批阶段
 */
export type ApprovalStage = 'POST_REASONING' | 'POST_ACTING';

/**
 * 审批结果
 */
export type ApprovalResultType = 'APPROVED' | 'REJECTED';

/**
 * CUSTOM 事件 HUMAN_APPROVAL 的 value 结构
 * 对应后端 HumanApprovalInfo record
 */
export interface HumanApprovalPayload {
  /** 审批阶段 */
  stage: ApprovalStage;
  /** 推理后暂停需要审批的信息 */
  reasoningStageInfo: ReasoningStageInfo[] | null;
  /** 行动后暂停需要审批的信息 */
  actingStageInfo: ActingStageInfo | null;
}

/**
 * POST_REASONING 阶段审批信息
 */
export interface ReasoningStageInfo {
  /** 提示文案 */
  tip: string;
  /** 待审批的工具调用信息 */
  toolInfo: {
    type: 'tool_use';
    id: string;
    name: string;
    input: Record<string, unknown>;
    content: string;
  };
}

/**
 * POST_ACTING 阶段审批信息
 */
export interface ActingStageInfo {
  /** 提示文案 */
  tip: string;
  /** 工具执行结果信息 */
  resultInfo: {
    type: 'tool_result';
    id: string;
    name: string;
    output: { type: string; text: string }[];
  };
}
```

- [ ] **Step 2: 创建状态管理文件 store.ts**

参照现有 `web-tool/dispatcher.ts` 的事件监听模式：

```ts
import type { HumanApprovalPayload } from './types';

/** 审批事件监听器列表 */
const approvalListeners = new Set<(payload: HumanApprovalPayload) => void>();

/** 当前待审批事件 */
let currentPendingApproval: HumanApprovalPayload | null = null;

/**
 * 分发人工审批事件
 * 由 CopilotKitProvider 中的 CUSTOM 事件监听调用
 */
export function dispatchHumanApproval(payload: HumanApprovalPayload): void {
  currentPendingApproval = payload;
  approvalListeners.forEach((listener) => listener(payload));
}

/**
 * 清除当前待审批事件
 * 用户完成审批后调用
 */
export function clearHumanApproval(): void {
  currentPendingApproval = null;
}

/**
 * 获取当前待审批事件
 */
export function getPendingApproval(): HumanApprovalPayload | null {
  return currentPendingApproval;
}

/**
 * 注册审批事件监听器（供 UI 组件使用）
 * @returns 取消监听的函数
 */
export function onHumanApproval(listener: (payload: HumanApprovalPayload) => void): () => void {
  approvalListeners.add(listener);

  // 如果已有待审批事件，立即通知
  if (currentPendingApproval) {
    listener(currentPendingApproval);
  }

  return () => {
    approvalListeners.delete(listener);
  };
}
```

- [ ] **Step 3: 创建导出文件 index.ts**

```ts
export type {
  ApprovalStage,
  ApprovalResultType,
  HumanApprovalPayload,
  ReasoningStageInfo,
  ActingStageInfo,
} from './types';

export {
  dispatchHumanApproval,
  clearHumanApproval,
  getPendingApproval,
  onHumanApproval,
} from './store';
```

- [ ] **Step 4: 提交**

```bash
git add web/apps/gwsu-main/src/services/human-approval/
git commit -m "feat(hitl): 前端新增审批类型定义和状态管理"
```

---

## Task 6: 前端 — CopilotKitProvider 扩展 HUMAN_APPROVAL 事件监听

**Files:**
- Modify: `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx`

- [ ] **Step 1: 修改 WebToolEventListener 中的 onCustomEvent 处理**

在 `WebToolEventListener` 组件的 `onCustomEvent` 回调中增加 `HUMAN_APPROVAL` 事件处理：

修改 `CopilotKitProvider.tsx` 中 `WebToolEventListener` 的 `subscriber` 对象：

```tsx
const subscriber = {
  onCustomEvent: ({ event }: { event: { name: string; value: unknown } }) => {
    if (event.name === 'TOOL_EXECUTE') {
      dispatchWebTool(event.value as WebToolExecutePayload);
    } else if (event.name === 'HUMAN_APPROVAL') {
      dispatchHumanApproval(event.value as HumanApprovalPayload);
    }
  },
};
```

添加 import：

```tsx
import { dispatchHumanApproval } from '@/services/human-approval';
import type { HumanApprovalPayload } from '@/services/human-approval';
```

- [ ] **Step 2: 验证前端编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm build:core 2>&1 | tail -5`
Expected: 编译成功

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx
git commit -m "feat(hitl): CopilotKitProvider 扩展 HUMAN_APPROVAL 自定义事件监听"
```

---

## Task 7: 前端 — 嵌入式审批卡片组件

**Files:**
- Create: `web/apps/gwsu-main/src/components/AIChat/HumanApprovalBar.tsx`
- Create: `web/apps/gwsu-main/src/components/AIChat/HumanApprovalBar.module.less`

- [ ] **Step 1: 创建审批卡片样式文件 HumanApprovalBar.module.less**

```less
/* 人工审批卡片 - 嵌入式展示在聊天输入框上方 */

.approvalBar {
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background: linear-gradient(
    135deg,
    oklch(97% 0.02 45) 0%,
    oklch(98% 0.01 45) 100%
  );
  border-top: 1px solid oklch(88% 0.05 45);
  border-bottom: 1px solid oklch(88% 0.05 45);
  animation: slideDown 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.approvalContent {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.approvalIcon {
  font-size: 16px;
  color: oklch(65% 0.18 45);
  margin-top: 2px;
  flex-shrink: 0;
}

.approvalInfo {
  flex: 1;
  min-width: 0;
}

.approvalTip {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color, #1a1a2e);
  line-height: 1.5;
  margin-bottom: 4px;
}

.approvalDetail {
  font-size: 12px;
  color: var(--text-secondary-color, #6b7280);
  line-height: 1.5;
}

.toolName {
  font-weight: 600;
  color: oklch(55% 0.15 45);
  margin-right: 4px;
}

.approvalActions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-top: 2px;
}

.approveBtn {
  font-size: 13px !important;
  height: 28px !important;
  padding: 0 12px !important;
  border-radius: 6px !important;
}

.rejectBtn {
  font-size: 13px !important;
  height: 28px !important;
  padding: 0 12px !important;
  border-radius: 6px !important;
}

/* 拒绝原因输入区域 */
.rejectReasonArea {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed oklch(88% 0.04 45);
  animation: slideDown 0.2s ease;
}

.rejectInput {
  flex: 1;
  font-size: 13px !important;
}

.rejectConfirmBtn {
  font-size: 13px !important;
  height: 32px !important;
  flex-shrink: 0;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
```

- [ ] **Step 2: 创建审批卡片组件 HumanApprovalBar.tsx**

```tsx
import { Button, Input, AlertOutlined } from '@ant-design/icons';
import { useState, useEffect, useCallback } from 'react';
import { onHumanApproval, clearHumanApproval, getPendingApproval } from '@/services/human-approval';
import type { HumanApprovalPayload, ApprovalResultType } from '@/services/human-approval';
import type { UseCopilotChatOptions } from '@copilotkit/react-core';
import { useCopilotChat } from '@copilotkit/react-core';
import { useAgent } from '@copilotkit/react-core/v2';
import styles from './HumanApprovalBar.module.less';

/**
 * 人工审批嵌入式卡片
 * 展示在聊天输入框上方，用户可同意或拒绝
 */
export function HumanApprovalBar() {
  const [pendingApproval, setPendingApproval] = useState<HumanApprovalPayload | null>(null);
  const [showRejectReason, setShowRejectReason] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { appendMessage, sendMessage } = useCopilotChat();
  const { agent } = useAgent({ agentId: 'brain' });

  // 监听审批事件
  useEffect(() => {
    // 初始化时检查是否已有待审批事件
    const existing = getPendingApproval();
    if (existing) {
      setPendingApproval(existing);
    }

    const unsubscribe = onHumanApproval((payload) => {
      setPendingApproval(payload);
      setShowRejectReason(false);
      setRejectReason('');
    });

    return unsubscribe;
  }, []);

  const submitApproval = useCallback(async (result: ApprovalResultType, reason?: string) => {
    if (!pendingApproval) return;

    setSubmitting(true);
    try {
      clearHumanApproval();
      setPendingApproval(null);
      setShowRejectReason(false);

      // 构造 approval 消息内容
      const approvalContent = result === 'REJECTED' && reason
        ? JSON.stringify({ result, rejectReason: reason })
        : JSON.stringify({ result });

      // 将 approval 消息追加到对话并发送
      // role: 'approval' — 后端识别后从 messages 中移除，不进入上下文历史
      await appendMessage({
        role: 'approval' as any,
        content: approvalContent,
        id: crypto.randomUUID(),
      } as any);
      await sendMessage();
    } catch (error) {
      console.error('[HumanApproval] 提交审批结果失败:', error);
    } finally {
      setSubmitting(false);
    }
  }, [pendingApproval, appendMessage, sendMessage]);

  const handleApprove = useCallback(() => {
    submitApproval('APPROVED');
  }, [submitApproval]);

  const handleReject = useCallback(() => {
    // POST_ACTING 阶段拒绝不需要原因，直接提交
    if (pendingApproval?.stage === 'POST_ACTING') {
      submitApproval('REJECTED');
    } else {
      // POST_REASONING 阶段展开原因输入
      setShowRejectReason(true);
    }
  }, [pendingApproval, submitApproval]);

  const handleRejectConfirm = useCallback(() => {
    submitApproval('REJECTED', rejectReason || undefined);
    setRejectReason('');
  }, [submitApproval, rejectReason]);

  const handleRejectCancel = useCallback(() => {
    setShowRejectReason(false);
    setRejectReason('');
  }, []);

  if (!pendingApproval) return null;

  const isPostReasoning = pendingApproval.stage === 'POST_REASONING';
  const tip = isPostReasoning
    ? pendingApproval.reasoningStageInfo?.[0]?.tip
    : pendingApproval.actingStageInfo?.tip;
  const toolName = isPostReasoning
    ? pendingApproval.reasoningStageInfo?.[0]?.toolInfo?.name
    : pendingApproval.actingStageInfo?.resultInfo?.name;

  return (
    <div className={styles.approvalBar}>
      <div className={styles.approvalContent}>
        <AlertOutlined className={styles.approvalIcon} />
        <div className={styles.approvalInfo}>
          <div className={styles.approvalTip}>{tip || '需要人工确认'}</div>
          {toolName && (
            <div className={styles.approvalDetail}>
              <span className={styles.toolName}>{toolName}</span>
              {isPostReasoning ? '待执行' : '已执行'}
            </div>
          )}
        </div>
        <div className={styles.approvalActions}>
          <Button
            type="primary"
            size="small"
            className={styles.approveBtn}
            onClick={handleApprove}
            loading={submitting}
          >
            同意
          </Button>
          <Button
            danger
            size="small"
            className={styles.rejectBtn}
            onClick={handleReject}
            loading={submitting}
          >
            拒绝
          </Button>
        </div>
      </div>
      {showRejectReason && (
        <div className={styles.rejectReasonArea}>
          <Input.TextArea
            className={styles.rejectInput}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="拒绝原因（可选）"
            autoSize={{ minRows: 1, maxRows: 3 }}
            maxLength={200}
          />
          <Button
            type="primary"
            danger
            size="small"
            className={styles.rejectConfirmBtn}
            onClick={handleRejectConfirm}
            loading={submitting}
          >
            确认拒绝
          </Button>
          <Button
            size="small"
            className={styles.rejectConfirmBtn}
            onClick={handleRejectCancel}
          >
            取消
          </Button>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-main/src/components/AIChat/HumanApprovalBar.tsx
git add web/apps/gwsu-main/src/components/AIChat/HumanApprovalBar.module.less
git commit -m "feat(hitl): 新增嵌入式审批卡片组件 HumanApprovalBar"
```

---

## Task 8: 前端 — CopilotChatPanel 集成审批卡片

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx`

- [ ] **Step 1: 在 CopilotChatPanel 中导入并放置 HumanApprovalBar**

添加 import：

```tsx
import { HumanApprovalBar } from './HumanApprovalBar';
```

在 `renderChatContent` 方法中，将 `HumanApprovalBar` 放置在 `CopilotChat` 组件之前（即 Header 和 CopilotChat 之间）：

修改 `renderChatContent` 为：

```tsx
const renderChatContent = () => (
  <>
    {/* 自定义 Header - 包含拖动和关闭按钮 */}
    <div className={`${styles.chatHeader} ${isDragging ? styles.dragging : ''}`}>
      {/* ... 保持原有 header 代码不变 ... */}
    </div>
    {/* 人工审批卡片 - 展示在聊天输入框上方 */}
    <HumanApprovalBar />
    {/* CopilotChat 组件 - 隐藏默认 header */}
    <CopilotChat
      labels={{
        title: '智能助手',
        placeholder: '输入消息...',
        initial: '我是你的平台助手，有什么问题可以问我哦^_^',
      }}
      className={styles.copilotChat}
    />
  </>
);
```

> 注意：`HumanApprovalBar` 位于 `copilotChatWrapper` 的 flex 容器中，在 Header 和 CopilotChat 之间。由于 `copilotChatWrapper` 是 `flex-direction: column`，审批卡片会自然地出现在消息列表和输入框之间（因为 CopilotChat 组件内部包含消息列表和输入框，审批卡片在 CopilotChat 外部上方）。

- [ ] **Step 2: 验证前端编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web/apps/gwsu-main && npx tsc --noEmit 2>&1 | head -20`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx
git commit -m "feat(hitl): CopilotChatPanel 集成审批卡片组件"
```

---

## Task 9: 前端 — brain.ts 新增审批状态查询 API

**Files:**
- Modify: `web/apps/gwsu-main/src/services/brain.ts`

- [ ] **Step 1: 添加 getApprovalStatus API**

在 `brain.ts` 文件末尾添加：

```ts
/**
 * 审批状态信息
 */
export interface ApprovalStatusInfo {
  /** 审批阶段 */
  stage: 'POST_REASONING' | 'POST_ACTING' | null;
  /** 推理后暂停需要审批的信息 */
  reasoningStageInfo: {
    tip: string;
    toolInfo: {
      type: 'tool_use';
      id: string;
      name: string;
      input: Record<string, unknown>;
      content: string;
    };
  }[] | null;
  /** 行动后暂停需要审批的信息 */
  actingStageInfo: {
    tip: string;
    resultInfo: {
      type: 'tool_result';
      id: string;
      name: string;
      output: { type: string; text: string }[];
    };
  } | null;
}

/**
 * 查询会话审批状态
 * 用于页面刷新后恢复审批卡片
 */
export async function getApprovalStatus(threadId: string): Promise<ApprovalStatusInfo> {
  const response = await get<ApprovalStatusInfo>(
    `/security/brain/approval/status/${threadId}`
  );
  return response.data;
}
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-main/src/services/brain.ts
git commit -m "feat(hitl): brain.ts 新增审批状态查询 API"
```

---

## Task 10: 前端 — 历史会话恢复审批状态

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx`

- [ ] **Step 1: 在 handleLoadSession 中增加审批状态恢复逻辑**

修改 `handleLoadSession` 方法，在 `agent.setMessages(formattedMessages)` 之后添加审批状态检查：

```tsx
const handleLoadSession = async (sessionId: string) => {
  try {
    const messages = await getSessionMessages(sessionId);
    reset();
    agent.threadId = sessionId;
    setCurrentThreadId(sessionId);
    const formattedMessages = messages.map((msg: BrainMessage) => {
      const formatted: Record<string, unknown> = {
        id: msg.id,
        role: msg.role,
        content: typeof msg.content === 'string' ? msg.content : (msg.content ? JSON.stringify(msg.content) : ''),
      };
      if (msg.toolCalls && msg.toolCalls.length > 0) {
        formatted.toolCalls = msg.toolCalls;
      }
      if (msg.toolCallId) {
        formatted.toolCallId = msg.toolCallId;
      }
      return formatted;
    });
    agent.setMessages(formattedMessages);

    // 检查是否需要恢复审批状态
    // 最后一条assistant消息包含ToolUseBlock但无对应ToolResultBlock时，查询审批状态
    const lastAssistantMsg = [...messages].reverse().find(
      (msg: BrainMessage) => msg.role === 'assistant' && msg.toolCalls && msg.toolCalls.length > 0
    );
    if (lastAssistantMsg) {
      try {
        const approvalStatus = await getApprovalStatus(sessionId);
        if (approvalStatus.stage) {
          dispatchHumanApproval(approvalStatus as any);
        }
      } catch (e) {
        // 审批状态查询失败不影响正常加载
        console.warn('[HumanApproval] 查询审批状态失败:', e);
      }
    }
  } catch (error) {
    console.error('加载会话消息失败:', error);
    message.error('加载会话消息失败');
  }
};
```

添加 import：

```tsx
import { getApprovalStatus } from '@/services/brain';
import { dispatchHumanApproval } from '@/services/human-approval';
```

- [ ] **Step 2: 验证前端编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web/apps/gwsu-main && npx tsc --noEmit 2>&1 | head -20`
Expected: 无类型错误

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx
git commit -m "feat(hitl): 历史会话加载时恢复审批状态"
```

---

## Task 11: 端到端验证

- [ ] **Step 1: 后端全量编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn clean compile -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端全量编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm build:core && pnpm build:main 2>&1 | tail -10`
Expected: 编译成功

- [ ] **Step 3: 启动后端服务，验证审批状态查询接口**

Run: `curl -s http://localhost:port/security/brain/approval/status/test-thread-id -H "Authorization: Bearer TOKEN" | jq .`
Expected: 返回 `{"code":200,"data":{"stage":null,...}}`

- [ ] **Step 4: 启动前端服务，触发智能助手对话，验证HITL流程**

1. 在智能助手中输入需要触发 `@HumanInTheLoop` 工具的对话
2. 验证：SSE流结束后，聊天输入框上方出现审批卡片
3. 点击"同意"：验证 Agent 继续执行
4. 重复步骤1，点击"拒绝"：验证 POST_REASONING 阶段展开原因输入框
5. 填写拒绝原因后确认：验证 Agent 收到拒绝信息
6. 刷新页面，从历史记录加载该会话：验证审批卡片恢复

- [ ] **Step 5: 最终提交**

如有修复，提交所有改动：

```bash
git add -A
git commit -m "fix(hitl): 端到端验证修复"
```
