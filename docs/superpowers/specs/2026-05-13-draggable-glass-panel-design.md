# 拖拽模式毛玻璃透明面板设计

## 概述

智能助手弹窗在拖拽模式下，整体切换为半透明毛玻璃效果，让用户可以看到弹窗背后的桌面内容，同时保持聊天功能的可用性。

## 需求确认

- **透明程度**：半透明毛玻璃（backdrop-filter: blur）
- **触发时机**：拖拽模式下始终透明，固定模式不受影响
- **方案选择**：方案A — 整体毛玻璃（Header + 消息区 + 输入框全部半透明）
- **模糊强度**：中等模糊（blur(16px) + rgba(255,255,255,0.2)）

## 改动范围

仅修改 `web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less`，不改动组件逻辑。

## 具体样式变更

### 1. `.draggableWrapper` 容器

| 属性 | 当前值 | 新值 |
|------|--------|------|
| background | 继承自 .copilotChatWrapper 的 var(--surface-color) | rgba(255,255,255,0.2) |
| backdrop-filter | 无 | blur(16px) |
| -webkit-backdrop-filter | 无 | blur(16px) |
| border | 1px solid var(--border-color, rgba(0,0,0,0.06)) | 1px solid rgba(255,255,255,0.3) |
| box-shadow | 0 4px 6px..., 0 10px 15px..., 0 20px 25px... | 0 8px 32px rgba(0,0,0,0.12) |

### 2. `.chatHeader` 头部区域（拖拽模式下）

| 属性 | 当前值 | 新值 |
|------|--------|------|
| background | linear-gradient(to bottom, var(--surface-color), oklch(98%...)) | rgba(255,255,255,0.15) |
| border-bottom | 1px solid var(--border-color, rgba(0,0,0,0.06)) | 1px solid rgba(255,255,255,0.2) |
| .chatHeaderTitle color | var(--text-color, #1a1a2e) | #fff |
| .actionButton color | var(--text-secondary-color, #6b7280) | rgba(255,255,255,0.8) |
| .actionButton:hover background | oklch(95% 0.02 var(--brand-hue)) | rgba(255,255,255,0.2) |
| .actionButton:hover color | var(--primary-color, #1a5fb4) | #fff |

### 3. 消息列表区域（拖拽模式下）

| 属性 | 当前值 | 新值 |
|------|--------|------|
| .copilot-chat-messages background | var(--background-color, #f8fafc) | transparent |
| 用户消息背景 | linear-gradient(135deg, var(--primary-color)...) | rgba(99,102,241,0.5) |
| 助手消息背景 | var(--surface-color, #ffffff) | rgba(255,255,255,0.25) |
| 助手消息 border | 1px solid var(--border-color, rgba(0,0,0,0.06)) | 1px solid rgba(255,255,255,0.2) |
| 助手消息 color | var(--text-color, #1a1a2e) | #fff |

### 4. 输入区域（拖拽模式下）

| 属性 | 当前值 | 新值 |
|------|--------|------|
| .copilot-chat-input-container background | var(--surface-color, #ffffff) | rgba(255,255,255,0.15) |
| .copilot-chat-input-container border-top | 1px solid var(--border-color, rgba(0,0,0,0.06)) | 1px solid rgba(255,255,255,0.2) |
| .copilot-chat-input background | var(--background-color, #f8fafc) | rgba(255,255,255,0.15) |
| .copilot-chat-input border | 1px solid var(--border-color, rgba(0,0,0,0.08)) | 1px solid rgba(255,255,255,0.2) |
| textarea color | var(--text-color, #1a1a2e) | #fff |
| textarea placeholder color | var(--text-secondary-color, #9ca3af) | rgba(255,255,255,0.5) |
| .copilot-chat-input:focus-within border | var(--primary-color) | rgba(255,255,255,0.5) |
| .copilot-chat-input:focus-within box-shadow | 0 0 0 3px rgba(26,95,180,0.1) | 0 0 0 3px rgba(255,255,255,0.1) |

### 5. AI模式控制条（拖拽模式下）

| 属性 | 当前值 | 新值 |
|------|--------|------|
| .controlBar background | linear-gradient(135deg, oklch(97% 0.03 25)...) | rgba(255,255,255,0.15) |
| .controlBar border | oklch(82% 0.08 25) | rgba(255,255,255,0.2) |
| .controlText color | oklch(40% 0.08 25) | rgba(255,255,255,0.9) |

### 6. 暗色主题兼容（midnight）

当使用暗色主题时，通过父级主题类名（如 `midnight` 相关的 CSS 变量）自动适配：
- 毛玻璃背景：`rgba(0,0,0,0.3)` 替代 `rgba(255,255,255,0.2)`
- 文字保持浅色（已经是白色系）
- 边框：`rgba(255,255,255,0.15)` 替代 `rgba(255,255,255,0.3)`

## 不影响固定模式

所有毛玻璃样式仅在 `.draggableWrapper` 作用域下生效，固定模式（`.copilotChatWrapper`）保持现有样式不变。

## 实现原则

1. 仅修改样式文件，不改组件逻辑
2. 通过 CSS 嵌套选择器限定拖拽模式作用域
3. 暗色主题通过现有 CSS 变量体系自动适配
4. 毛玻璃效果需要 `-webkit-backdrop-filter` 兼容 Safari
