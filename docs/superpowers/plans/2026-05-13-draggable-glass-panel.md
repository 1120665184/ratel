# 拖拽模式毛玻璃透明面板 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 智能助手弹窗在拖拽模式下切换为整体半透明毛玻璃效果，让用户能看到弹窗背后的桌面内容。

**Architecture:** 仅修改 CSS 样式文件，通过 `.draggableWrapper` 作用域下的嵌套选择器覆盖原有样式，使用 `backdrop-filter: blur(16px)` 实现毛玻璃，暗色主题通过 `[data-theme="midnight"]` 选择器适配。

**Tech Stack:** Less (CSS Modules)、backdrop-filter、CSS Variables

---

### Task 1: 修改拖拽模式容器为毛玻璃背景

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less:29-48`

- [ ] **Step 1: 修改 `.draggableWrapper` 样式为毛玻璃背景**

将 `.draggableWrapper` 的背景、边框、阴影替换为毛玻璃样式：

```less
/* 拖拽模式下的容器样式 - 毛玻璃效果 */
.draggableWrapper {
  /* 覆盖固定模式的定位样式 */
  top: 80px;
  left: 20px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.3);
  z-index: 100000;
  resize: both;
  overflow: hidden;
  min-width: 380px;
  min-height: 480px;
  pointer-events: auto;
  border-right: none;
  width: 420px;
  height: 520px;

  /* 毛玻璃核心样式 */
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);

  /* 暗色主题适配 */
  :global([data-theme="midnight"]) & {
    background: rgba(0, 0, 0, 0.3);
    border-color: rgba(255, 255, 255, 0.15);
  }
}
```

- [ ] **Step 2: 启动开发服务器验证容器毛玻璃效果**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm dev:main`
Expected: 切换到拖拽模式后，面板容器背景呈半透明毛玻璃效果，能看到背后桌面

- [ ] **Step 3: Commit**

```bash
git add web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less
git commit -m "feat: 拖拽模式容器添加毛玻璃背景效果"
```

---

### Task 2: Header 头部区域毛玻璃适配

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less:58-122`

- [ ] **Step 1: 在 `.draggableWrapper` 内添加 Header 毛玻璃覆盖样式**

在 `.draggableWrapper` 的花括号内部（上一步修改的样式块内），追加以下嵌套选择器：

```less
  /* Header 毛玻璃覆盖 */
  .chatHeader {
    background: rgba(255, 255, 255, 0.15);
    border-bottom-color: rgba(255, 255, 255, 0.2);
  }

  .chatHeaderTitle {
    color: #fff;

    svg {
      color: #fff;
      opacity: 0.9;
    }
  }

  .actionButton {
    color: rgba(255, 255, 255, 0.8);

    &:hover {
      background: rgba(255, 255, 255, 0.2);
      color: #fff;
    }

    &.active {
      background: rgba(255, 255, 255, 0.3);
      color: #fff;
    }
  }
```

- [ ] **Step 2: 在暗色主题适配块中追加 Header 暗色覆盖**

在 Task 1 中添加的 `:global([data-theme="midnight"]) &` 块内追加：

```less
    .chatHeader {
      background: rgba(0, 0, 0, 0.2);
      border-bottom-color: rgba(255, 255, 255, 0.1);
    }
```

- [ ] **Step 3: 启动开发服务器验证 Header 毛玻璃效果**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm dev:main`
Expected: 拖拽模式下 Header 背景半透明，标题和按钮文字为白色，鼠标悬停按钮有白色半透明背景

- [ ] **Step 4: Commit**

```bash
git add web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less
git commit -m "feat: 拖拽模式Header区域毛玻璃适配"
```

---

### Task 3: 消息列表区域毛玻璃适配

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less:124-217`

- [ ] **Step 1: 在 `.draggableWrapper` 内添加消息区域毛玻璃覆盖样式**

在 `.draggableWrapper` 的花括号内部追加：

```less
  /* 消息列表毛玻璃覆盖 */
  .copilotChat {
    :global(.copilot-chat-messages) {
      background: transparent;

      &::-webkit-scrollbar-thumb {
        background: rgba(255, 255, 255, 0.3);

        &:hover {
          background: rgba(255, 255, 255, 0.4);
        }
      }
    }

    :global(.copilot-message-user-content) {
      background: rgba(99, 102, 241, 0.5);
      border: 1px solid rgba(99, 102, 241, 0.3);
      box-shadow: 0 2px 12px rgba(99, 102, 241, 0.15);
    }

    :global(.copilot-message-assistant-content) {
      background: rgba(255, 255, 255, 0.25);
      color: #fff;
      border-color: rgba(255, 255, 255, 0.2);
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    }
  }
```

- [ ] **Step 2: 在暗色主题适配块中追加消息区域暗色覆盖**

在 `:global([data-theme="midnight"]) &` 块内追加：

```less
    .copilotChat {
      :global(.copilot-message-assistant-content) {
        background: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.1);
      }

      :global(.copilot-message-user-content) {
        background: rgba(96, 165, 250, 0.4);
        border-color: rgba(96, 165, 250, 0.3);
      }
    }
```

- [ ] **Step 3: 启动开发服务器验证消息区域效果**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm dev:main`
Expected: 拖拽模式下消息背景透明，用户消息半透明紫蓝色，助手消息半透明白色+白色文字

- [ ] **Step 4: Commit**

```bash
git add web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less
git commit -m "feat: 拖拽模式消息列表区域毛玻璃适配"
```

---

### Task 4: 输入区域毛玻璃适配

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less:219-294`

- [ ] **Step 1: 在 `.draggableWrapper` 内添加输入区域毛玻璃覆盖样式**

在 `.draggableWrapper` 的花括号内部追加：

```less
  /* 输入区域毛玻璃覆盖 */
  .copilotChat {
    :global(.copilot-chat-input-container) {
      background: rgba(255, 255, 255, 0.15);
      border-top-color: rgba(255, 255, 255, 0.2);
    }

    :global(.copilot-chat-input) {
      background: rgba(255, 255, 255, 0.15);
      border-color: rgba(255, 255, 255, 0.2);
    }

    :global(.copilot-chat-input:focus-within) {
      border-color: rgba(255, 255, 255, 0.5);
      box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
    }

    :global(.copilot-chat-input textarea) {
      color: #fff;
    }

    :global(.copilot-chat-input textarea::placeholder) {
      color: rgba(255, 255, 255, 0.5);
    }

    :global(.copilot-chat-send-button) {
      background: rgba(255, 255, 255, 0.25);
      color: #fff;

      &:hover:not(:disabled) {
        background: rgba(255, 255, 255, 0.35);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }

      &:disabled {
        background: rgba(255, 255, 255, 0.1);
      }
    }
  }
```

- [ ] **Step 2: 在暗色主题适配块中追加输入区域暗色覆盖**

在 `:global([data-theme="midnight"]) &` 块内追加：

```less
    .copilotChat {
      :global(.copilot-chat-input-container) {
        background: rgba(0, 0, 0, 0.2);
        border-top-color: rgba(255, 255, 255, 0.1);
      }

      :global(.copilot-chat-input) {
        background: rgba(0, 0, 0, 0.2);
        border-color: rgba(255, 255, 255, 0.1);
      }

      :global(.copilot-chat-input:focus-within) {
        border-color: rgba(255, 255, 255, 0.3);
        box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.05);
      }
    }
```

- [ ] **Step 3: 启动开发服务器验证输入区域效果**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm dev:main`
Expected: 拖拽模式下输入框背景半透明，文字白色，placeholder 半透明白色，发送按钮半透明白色

- [ ] **Step 4: Commit**

```bash
git add web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less
git commit -m "feat: 拖拽模式输入区域毛玻璃适配"
```

---

### Task 5: AI模式控制条与空状态/加载动画毛玻璃适配

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less`
- Modify: `web/apps/gwsu-main/src/components/AIChat/AiModeControlBar/index.module.less`

- [ ] **Step 1: 在 `.draggableWrapper` 内添加空状态和加载动画覆盖样式**

在 `.draggableWrapper` 的花括号内部追加：

```less
  /* 空状态毛玻璃覆盖 */
  .copilotChat {
    :global(.copilot-chat-empty) {
      color: rgba(255, 255, 255, 0.7);
    }

    :global(.copilot-chat-empty-icon) {
      color: #fff;
      opacity: 0.4;
    }

    :global(.copilot-chat-empty-title) {
      color: #fff;
    }

    :global(.copilot-chat-empty-subtitle) {
      color: rgba(255, 255, 255, 0.6);
    }

    /* 加载动画毛玻璃覆盖 */
    :global(.copilot-loading) {
      background: rgba(255, 255, 255, 0.25);
      border-color: rgba(255, 255, 255, 0.2);
    }

    :global(.copilot-loading-dot) {
      background: #fff;
    }
  }
```

- [ ] **Step 2: 修改 AiModeControlBar 样式支持拖拽模式毛玻璃**

在 `web/apps/gwsu-main/src/components/AIChat/AiModeControlBar/index.module.less` 文件末尾追加拖拽模式覆盖：

```less
/* 拖拽模式下毛玻璃覆盖 - 通过父级 .draggableWrapper 限定 */
:global(.draggableWrapper) .controlBar {
  background: rgba(255, 255, 255, 0.15);
  border-top-color: rgba(255, 255, 255, 0.2);
  border-bottom-color: rgba(255, 255, 255, 0.2);
}

:global(.draggableWrapper) .controlIcon {
  color: rgba(255, 255, 255, 0.9);
}

:global(.draggableWrapper) .controlText {
  color: rgba(255, 255, 255, 0.9);
}

/* 暗色主题拖拽模式 */
:global([data-theme="midnight"]:global(.draggableWrapper)) .controlBar {
  background: rgba(0, 0, 0, 0.2);
  border-top-color: rgba(255, 255, 255, 0.1);
  border-bottom-color: rgba(255, 255, 255, 0.1);
}
```

- [ ] **Step 3: 启动开发服务器验证所有组件效果**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm dev:main`
Expected: 拖拽模式下空状态、加载动画、AI控制条均适配毛玻璃风格

- [ ] **Step 4: Commit**

```bash
git add web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less web/apps/gwsu-main/src/components/AIChat/AiModeControlBar/index.module.less
git commit -m "feat: 拖拽模式AI控制条/空状态/加载动画毛玻璃适配"
```

---

### Task 6: 全面验证与最终提交

**Files:**
- All modified files from previous tasks

- [ ] **Step 1: 启动开发服务器进行全面验证**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm dev:main`

验证清单：
1. 固定模式下面板样式完全不受影响（无任何变化）
2. 拖拽模式下整体毛玻璃效果正常
3. Header 半透明 + 白色文字和按钮
4. 消息气泡半透明 + 白色文字
5. 输入框半透明 + 白色文字和 placeholder
6. AI 模式控制条半透明
7. 空状态和加载动画适配
8. 切换到午夜暗色主题后拖拽模式效果正常
9. 拖拽面板可正常移动和缩放

- [ ] **Step 2: 如有问题，修复并提交**

- [ ] **Step 3: 最终确认无问题后，合并所有提交（可选）**

```bash
git log --oneline -5  # 确认所有提交都在
```
