# AI 操作模式实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 WebTool 增加 AI 操作模式机制，AI 操作界面前需先进入 AI 模式（经用户审批），操作完成后主动退出。AI 模式下操作区被遮罩锁定，用户可通过终止组件强制回退人类模式。

**Architecture:** 后端新增 EnterAiMode/ExitAiMode 两个工具方法，移除 RouteNavigation 的审批注解；前端新增工具执行器、操作模式守卫、遮罩组件和终止控制组件，通过 forwardedProps 的 operationMode 属性贯穿前后端状态同步。

**Tech Stack:** Java/Spring Boot（后端）、React/Zustand/Ant Design（前端）、AG-UI 协议

---

## 文件变更清单

| 操作 | 文件路径 | 职责 |
|------|----------|------|
| 修改 | `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/service/tool/WebTool.java` | 新增 EnterAiMode/ExitAiMode 工具，移除 RouteNavigation 审批注解 |
| 创建 | `web/apps/gwsu-main/src/services/web-tool/tools/enter-ai-mode.ts` | EnterAiMode 前端执行器 |
| 创建 | `web/apps/gwsu-main/src/services/web-tool/tools/exit-ai-mode.ts` | ExitAiMode 前端执行器 |
| 修改 | `web/apps/gwsu-main/src/services/web-tool/dispatcher.ts` | 增加操作模式守卫 |
| 修改 | `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx` | 注册新工具 import |
| 创建 | `web/apps/gwsu-main/src/components/AiModeOverlay/index.tsx` | AI 模式遮罩组件 |
| 创建 | `web/apps/gwsu-main/src/components/AiModeOverlay/index.module.less` | 遮罩样式 |
| 修改 | `web/apps/gwsu-main/src/components/InterfaceOperation/index.tsx` | 引入遮罩组件 |
| 创建 | `web/apps/gwsu-main/src/components/AIChat/AiModeControlBar/index.tsx` | 终止控制组件 |
| 创建 | `web/apps/gwsu-main/src/components/AIChat/AiModeControlBar/index.module.less` | 终止控制样式 |
| 修改 | `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx` | 引入终止控制组件，调整排列顺序 |

---

### Task 1: 后端 — WebTool 新增工具与修改审批注解

**Files:**
- Modify: `business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/service/tool/WebTool.java`

- [ ] **Step 1: 移除 RouteNavigation 的 @HumanInTheLoop 注解**

将 `routeNavigation` 方法上的 `@HumanInTheLoop(tip = "是否同意路由跳转？")` 注解删除，方法签名和实现保持不变。

修改后的 `routeNavigation` 方法头部：

```java
@Tool(name = "RouteNavigation", description = "控制web界面跳转到指定路由")
public ToolResultBlock routeNavigation(@ToolParam(name = "path",
                                               description = """
                                                       跳转的前端路由地址
                                                       示例：/sub-system/user
                                                       """) String path,
                                       ToolEmitter emitter) throws TimeoutException {
```

- [ ] **Step 2: 新增 enterAiMode 工具方法**

在 `WebTool.java` 的 `// ==================== 查看界面 ====================` 注释之前添加：

```java
// ==================== 操作模式 ====================

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

- [ ] **Step 3: 新增 exitAiMode 工具方法**

紧接 `enterAiMode` 方法之后添加：

```java
@Tool(name = "ExitAiMode", description = """
        退出AI操作模式，将界面控制权交还给用户。
        当你完成所有界面操作后，必须调用此工具退出AI操作模式。""")
public ToolResultBlock exitAiMode(ToolEmitter emitter) throws TimeoutException {
    return webToolUtils.webExecuteTool(emitter, "ExitAiMode", Map.of());
}
```

- [ ] **Step 4: 验证编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn compile -pl business/business-security/business-security-server -am -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add business/business-security/business-security-server/src/main/java/org/quyq/gwsu/security/brain/service/tool/WebTool.java
git commit -m "feat: WebTool新增EnterAiMode/ExitAiMode工具，移除RouteNavigation审批注解"
```

---

### Task 2: 前端 — 新增 WebTool 执行器

**Files:**
- Create: `web/apps/gwsu-main/src/services/web-tool/tools/enter-ai-mode.ts`
- Create: `web/apps/gwsu-main/src/services/web-tool/tools/exit-ai-mode.ts`

- [ ] **Step 1: 创建 enter-ai-mode.ts**

```typescript
import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import { useForwardedPropsStore } from '@/stores/forwardedProps';

/**
 * 进入AI操作模式工具
 * 将操作模式切换为AI模式，锁定界面控制权
 */
const enterAiModeTool: WebToolExecutor = {
  async execute(): Promise<WebToolResult> {
    useForwardedPropsStore.getState().setOperationMode('ai');
    return { success: true, result: '已进入AI操作模式，界面控制权已交给智能助手' };
  },
};

registerWebTool('EnterAiMode', enterAiModeTool);
```

- [ ] **Step 2: 创建 exit-ai-mode.ts**

```typescript
import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import { useForwardedPropsStore } from '@/stores/forwardedProps';

/**
 * 退出AI操作模式工具
 * 将操作模式切换为人类模式，交还界面控制权
 */
const exitAiModeTool: WebToolExecutor = {
  async execute(): Promise<WebToolResult> {
    useForwardedPropsStore.getState().setOperationMode('human');
    return { success: true, result: '已退出AI操作模式，界面控制权已交还给用户' };
  },
};

registerWebTool('ExitAiMode', exitAiModeTool);
```

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-main/src/services/web-tool/tools/enter-ai-mode.ts web/apps/gwsu-main/src/services/web-tool/tools/exit-ai-mode.ts
git commit -m "feat: 新增EnterAiMode/ExitAiMode前端WebTool执行器"
```

---

### Task 3: 前端 — dispatcher 增加操作模式守卫

**Files:**
- Modify: `web/apps/gwsu-main/src/services/web-tool/dispatcher.ts`

- [ ] **Step 1: 添加 import 和白名单常量**

在文件顶部 import 区域添加：

```typescript
import { useForwardedPropsStore } from '@/stores/forwardedProps';
```

在 `confirmListeners` 声明之前添加白名单常量：

```typescript
/** 需要 AI 操作模式的工具列表 */
const AI_MODE_REQUIRED_TOOLS = [
  'RouteNavigation',
  'ClickElement',
  'InputText',
  'SelectOption',
  'ScrollPage',
];
```

- [ ] **Step 2: 在 dispatchWebTool 中添加守卫逻辑**

在 `dispatchWebTool` 函数中，`const executor = getWebTool(toolName);` 之前插入守卫检查：

```typescript
export async function dispatchWebTool(payload: WebToolExecutePayload): Promise<void> {
  const { toolCallId, toolName, params } = payload;

  // 操作模式守卫：需要AI模式的工具在人类模式下直接拒绝
  const mode = useForwardedPropsStore.getState().operationMode;
  if (mode === 'human' && AI_MODE_REQUIRED_TOOLS.includes(toolName)) {
    await callbackToolResult(toolCallId, false, '当前为人类操作模式，请先进入AI操作模式');
    return;
  }

  const executor = getWebTool(toolName);
  // ... 后续逻辑不变
```

- [ ] **Step 3: 提交**

```bash
git add web/apps/gwsu-main/src/services/web-tool/dispatcher.ts
git commit -m "feat: WebTool dispatcher增加操作模式守卫，AI模式工具白名单校验"
```

---

### Task 4: 前端 — CopilotKitProvider 注册新工具

**Files:**
- Modify: `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx`

- [ ] **Step 1: 添加 import 语句**

在现有工具 import 区域（`import '@/services/web-tool/tools/scroll-page';` 之后）添加：

```typescript
// 确保 AI 操作模式工具被注册
import '@/services/web-tool/tools/enter-ai-mode';
import '@/services/web-tool/tools/exit-ai-mode';
```

- [ ] **Step 2: 提交**

```bash
git add web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx
git commit -m "feat: CopilotKitProvider注册EnterAiMode/ExitAiMode工具"
```

---

### Task 5: 前端 — AI 模式遮罩组件

**Files:**
- Create: `web/apps/gwsu-main/src/components/AiModeOverlay/index.tsx`
- Create: `web/apps/gwsu-main/src/components/AiModeOverlay/index.module.less`
- Modify: `web/apps/gwsu-main/src/components/InterfaceOperation/index.tsx`

- [ ] **Step 1: 创建遮罩样式 index.module.less**

```less
/* AI操作模式遮罩 - 覆盖操作区+弹框 */

.overlay {
  position: fixed;
  inset: 0;
  z-index: 1050;
  background: oklch(0% 0 0 / 0.25);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: not-allowed;
}

.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 48px;
  background: oklch(100% 0 0 / 0.92);
  border-radius: 16px;
  box-shadow:
    0 8px 32px oklch(0% 0 0 / 0.12),
    0 2px 8px oklch(0% 0 0 / 0.06);
  pointer-events: auto;
}

.icon {
  font-size: 36px;
  color: oklch(62% 0.19 25);
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.text {
  font-size: 16px;
  font-weight: 600;
  color: oklch(35% 0.08 25);
  letter-spacing: 0.02em;
}

.subText {
  font-size: 13px;
  color: oklch(55% 0.04 25);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}
```

- [ ] **Step 2: 创建遮罩组件 index.tsx**

```tsx
import { RobotOutlined } from '@ant-design/icons';
import { Spin } from 'antd';
import { createPortal } from 'react-dom';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import styles from './index.module.less';

/**
 * AI操作模式遮罩组件
 * 当 operationMode 为 'ai' 时，全屏覆盖操作区和弹框
 * 使用 createPortal 挂载到 body，z-index=1050（高于Ant Design Modal，低于聊天面板）
 */
export function AiModeOverlay() {
  const operationMode = useForwardedPropsStore((s) => s.operationMode);

  if (operationMode !== 'ai') return null;

  return createPortal(
    <div className={styles.overlay}>
      <div className={styles.content}>
        <RobotOutlined className={styles.icon} />
        <Spin size="large" />
        <div className={styles.text}>智能助手控制中...</div>
        <div className={styles.subText}>请在右侧助手面板操作</div>
      </div>
    </div>,
    document.body,
  );
}
```

- [ ] **Step 3: 在 InterfaceOperation 中引入遮罩组件**

修改 `web/apps/gwsu-main/src/components/InterfaceOperation/index.tsx`：

添加 import：

```typescript
import { AiModeOverlay } from '@/components/AiModeOverlay';
```

在 return 的 JSX 中，在 `<div className={styles.interfaceOperation}` 的闭合标签 `</div>` 之前添加：

```tsx
{/* AI操作模式遮罩 */}
<AiModeOverlay />
```

- [ ] **Step 4: 提交**

```bash
git add web/apps/gwsu-main/src/components/AiModeOverlay/ web/apps/gwsu-main/src/components/InterfaceOperation/index.tsx
git commit -m "feat: 新增AI模式遮罩组件，InterfaceOperation中引入"
```

---

### Task 6: 前端 — 终止控制组件 AiModeControlBar

**Files:**
- Create: `web/apps/gwsu-main/src/components/AIChat/AiModeControlBar/index.tsx`
- Create: `web/apps/gwsu-main/src/components/AIChat/AiModeControlBar/index.module.less`
- Modify: `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx`

- [ ] **Step 1: 创建终止控制样式 index.module.less**

```less
/* AI模式终止控制条 - 嵌入式展示在聊天输入框上方 */

.controlBar {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: linear-gradient(
    135deg,
    oklch(97% 0.03 25) 0%,
    oklch(96% 0.04 25) 100%
  );
  border-top: 1px solid oklch(82% 0.08 25);
  border-bottom: 1px solid oklch(82% 0.08 25);
  animation: slideDown 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.controlIcon {
  font-size: 16px;
  color: oklch(60% 0.2 25);
  margin-right: 8px;
  flex-shrink: 0;
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.controlInfo {
  flex: 1;
  min-width: 0;
}

.controlText {
  font-size: 13px;
  font-weight: 500;
  color: oklch(40% 0.08 25);
  line-height: 1.5;
}

.stopBtn {
  font-size: 13px !important;
  height: 28px !important;
  padding: 0 14px !important;
  border-radius: 6px !important;
  font-weight: 500 !important;
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

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
```

- [ ] **Step 2: 创建终止控制组件 index.tsx**

```tsx
import { StopOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import { useCallback } from 'react';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import styles from './index.module.less';

/**
 * AI模式终止控制组件
 * 展示在聊天面板输入框上方，用户可随时终止AI控制
 * 仅当 operationMode === 'ai' 时渲染
 */
export function AiModeControlBar() {
  const operationMode = useForwardedPropsStore((s) => s.operationMode);
  const setOperationMode = useForwardedPropsStore((s) => s.setOperationMode);

  const handleStop = useCallback(() => {
    setOperationMode('human');
  }, [setOperationMode]);

  if (operationMode !== 'ai') return null;

  return (
    <div className={styles.controlBar}>
      <StopOutlined className={styles.controlIcon} />
      <div className={styles.controlInfo}>
        <span className={styles.controlText}>智能助手正在控制界面</span>
      </div>
      <Button
        danger
        size="small"
        className={styles.stopBtn}
        icon={<StopOutlined />}
        onClick={handleStop}
      >
        终止控制
      </Button>
    </div>
  );
}
```

- [ ] **Step 3: 在 CopilotChatPanel 中引入终止控制组件**

修改 `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx`：

添加 import：

```typescript
import { AiModeControlBar } from './AiModeControlBar';
```

在 `renderChatContent` 函数中，修改组件排列顺序为 AiModeControlBar → HumanApprovalBar → AskUserQuestionBar：

将原来的：
```tsx
{/* 人工审批卡片 - 展示在聊天输入框上方 */}
<HumanApprovalBar />
{/* AskUserQuestion 选择框 - 展示在审批卡片下方 */}
<AskUserQuestionBar />
```

改为：
```tsx
{/* AI模式终止控制 - 最上方 */}
<AiModeControlBar />
{/* 人工审批卡片 */}
<HumanApprovalBar />
{/* AskUserQuestion 选择框 */}
<AskUserQuestionBar />
```

- [ ] **Step 4: 提交**

```bash
git add web/apps/gwsu-main/src/components/AIChat/AiModeControlBar/ web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx
git commit -m "feat: 新增AI模式终止控制组件，CopilotChatPanel中引入并调整排列顺序"
```

---

### Task 7: 集成验证

- [ ] **Step 1: 后端编译验证**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic && mvn compile -pl business/business-security/business-security-server -am -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端编译验证**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm build:main`
Expected: 构建成功，无 TS 报错

- [ ] **Step 3: 功能流程验证（手动）**

启动前后端后验证以下流程：
1. 在聊天面板发送"帮我点击用户管理按钮"
2. AI 应先调用 EnterAiMode → 弹出审批卡片
3. 批准后 → 操作区出现遮罩"智能助手控制中..."，聊天面板出现终止控制条
4. AI 继续调用 GetPageState → ClickElement（ClickElement 仍触发 HumanInTheLoop 审批）
5. AI 操作完成后调用 ExitAiMode → 遮罩消失，终止控制条消失
6. 测试终止：重复步骤1-3，在 AI 操作过程中点击"终止控制" → 遮罩消失，后续 WebTool 被守卫拒绝
