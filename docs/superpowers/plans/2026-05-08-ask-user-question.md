# AskUserQuestion 弹框功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为智能助手添加 AskUserQuestion 弹框，监听 AG-UI ToolCallEndEvent 触发选择框，用户作答后以 role:'tool' 消息提交并恢复 Agent 执行。

**Architecture:** 前端通过 CopilotKit 的 agent.subscribe 监听 onToolCallEndEvent，检测 AskUserQuestion 工具后 dispatch 事件到 store，AskUserQuestionBar 组件消费 store 状态弹出分步式选择框。用户作答后构造 role:'tool' 标准消息调用 agent.runAgent() 恢复执行。弹框与输入框互斥，任一弹框活跃时输入框禁用。

**Tech Stack:** React 18 / TypeScript / Ant Design 6 / CopilotKit / AG-UI Protocol

---

## 文件结构

### 前端新增

| 文件 | 职责 |
|------|------|
| `web/apps/gwsu-main/src/services/ask-user-question/types.ts` | AskUserQuestion 类型定义 |
| `web/apps/gwsu-main/src/services/ask-user-question/store.ts` | 状态管理（事件分发、监听） |
| `web/apps/gwsu-main/src/services/ask-user-question/index.ts` | 导出 |
| `web/apps/gwsu-main/src/components/AIChat/AskUserQuestionBar.tsx` | 分步式选择框组件 |
| `web/apps/gwsu-main/src/components/AIChat/AskUserQuestionBar.module.less` | 选择框样式 |

### 前端修改

| 文件 | 职责 |
|------|------|
| `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx` | 增加 onToolCallEndEvent 监听 |
| `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx` | 集成 AskUserQuestionBar + 互斥控制 + 历史恢复 |
| `web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less` | 互斥时禁用输入框的 CSS |

---

## Task 1: 创建 ask-user-question 服务层

**Files:**
- Create: `web/apps/gwsu-main/src/services/ask-user-question/types.ts`
- Create: `web/apps/gwsu-main/src/services/ask-user-question/store.ts`
- Create: `web/apps/gwsu-main/src/services/ask-user-question/index.ts`

- [ ] **Step 1: 创建类型定义文件 types.ts**

```ts
/**
 * 问题选项
 */
export interface QuestionOption {
  label: string;
  description: string;
}

/**
 * 问题参数（对应后端 QuestionParam）
 */
export interface QuestionParam {
  question: string;
  header: string;
  options: QuestionOption[];
  multiSelect: boolean;
}

/**
 * AskUserQuestion 事件载荷
 */
export interface AskUserQuestionPayload {
  /** 工具调用唯一标识，用于构造 tool 结果消息 */
  toolCallId: string;
  /** 问题列表（1-4个） */
  questions: QuestionParam[];
}

/**
 * 用户作答结果
 */
export interface AskUserQuestionAnswer {
  /** key=question, value=选中的label（多选时逗号分隔） */
  answers: Record<string, string>;
  /** 可选的备注信息 */
  annotations: Record<string, { preview?: string; notes?: string }>;
}
```

- [ ] **Step 2: 创建状态管理文件 store.ts**

```ts
import type { AskUserQuestionPayload } from './types';

/** 事件监听器列表，payload 为 null 表示清除状态 */
const listeners = new Set<(payload: AskUserQuestionPayload | null) => void>();

/** 当前待回答问题 */
let currentPending: AskUserQuestionPayload | null = null;

/**
 * 分发 AskUserQuestion 事件
 * 由 CopilotKitProvider 中的 ToolCallEndEvent 监听调用
 */
export function dispatchAskUserQuestion(payload: AskUserQuestionPayload): void {
  currentPending = payload;
  listeners.forEach((listener) => listener(payload));
}

/**
 * 清除当前待回答事件，并通知所有监听器
 * 用户完成作答、新建会话、切换会话时调用
 */
export function clearAskUserQuestion(): void {
  currentPending = null;
  listeners.forEach((listener) => listener(null));
}

/**
 * 获取当前待回答事件
 */
export function getPendingAskUserQuestion(): AskUserQuestionPayload | null {
  return currentPending;
}

/**
 * 注册事件监听器（供 UI 组件使用）
 * payload 为 null 时表示状态已清除
 * @returns 取消监听的函数
 */
export function onAskUserQuestion(listener: (payload: AskUserQuestionPayload | null) => void): () => void {
  listeners.add(listener);

  // 如果已有待回答事件，立即通知
  if (currentPending) {
    listener(currentPending);
  }

  return () => {
    listeners.delete(listener);
  };
}
```

- [ ] **Step 3: 创建导出文件 index.ts**

```ts
export type {
  QuestionOption,
  QuestionParam,
  AskUserQuestionPayload,
  AskUserQuestionAnswer,
} from './types';

export {
  dispatchAskUserQuestion,
  clearAskUserQuestion,
  getPendingAskUserQuestion,
  onAskUserQuestion,
} from './store';
```

- [ ] **Step 4: 验证前端编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm build:core 2>&1 | tail -5`
Expected: 编译成功

- [ ] **Step 5: 提交**

```bash
git add web/apps/gwsu-main/src/services/ask-user-question/
git commit -m "feat(ask-user-question): 新增类型定义和状态管理"
```

---

## Task 2: CopilotKitProvider 增加 ToolCallEndEvent 监听

**Files:**
- Modify: `web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx`

- [ ] **Step 1: 添加 import**

在 `CopilotKitProvider.tsx` 顶部的 import 区域添加：

```ts
import { dispatchAskUserQuestion } from '@/services/ask-user-question';
import type { AskUserQuestionPayload, QuestionParam, QuestionOption } from '@/services/ask-user-question';
```

- [ ] **Step 2: 在 WebToolEventListener 的 subscriber 中添加 onToolCallEndEvent 处理**

将 `WebToolEventListener` 组件中的 `subscriber` 对象扩展：

```ts
const subscriber: AgentSubscriber = {
  onCustomEvent: ({ event }):void => {
    if (event.name === 'TOOL_EXECUTE') {
      dispatchWebTool(event.value as WebToolExecutePayload);
    } else if (event.name === 'HUMAN_APPROVAL') {
      dispatchHumanApproval(event.value as HumanApprovalPayload);
    }
  },
  onToolCallEndEvent: ({ toolCallName, toolCallArgs, event }): void => {
    if (toolCallName === 'AskUserQuestion') {
      // 解析 questions 参数，兼容 options 为数组或单对象的情况
      const rawQuestions = toolCallArgs?.questions;
      if (Array.isArray(rawQuestions) && rawQuestions.length > 0) {
        const questions: QuestionParam[] = rawQuestions.map((q: Record<string, unknown>) => ({
          question: String(q.question ?? ''),
          header: String(q.header ?? ''),
          options: normalizeOptions(q.options),
          multiSelect: Boolean(q.multiSelect),
        }));
        dispatchAskUserQuestion({
          toolCallId: event.toolCallId,
          questions,
        });
      }
    }
  },
};
```

- [ ] **Step 3: 在 WebToolEventListener 组件函数体内添加 normalizeOptions 辅助函数**

在 `WebToolEventListener` 函数体内部、`useEffect` 之前添加：

```ts
/**
 * 规范化 options 字段
 * 后端 QuestionParam.options 类型为 QuestionOption（单对象），
 * 但 LLM 根据 description 会生成数组，前端兼容两种情况
 */
const normalizeOptions = (options: unknown): QuestionOption[] => {
  if (Array.isArray(options)) return options as QuestionOption[];
  if (options && typeof options === 'object') return [options as QuestionOption];
  return [];
};
```

- [ ] **Step 4: 验证前端编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web/apps/gwsu-main && npx tsc --noEmit 2>&1 | head -20`
Expected: 无类型错误

- [ ] **Step 5: 提交**

```bash
git add web/apps/gwsu-main/src/providers/CopilotKitProvider.tsx
git commit -m "feat(ask-user-question): CopilotKitProvider 监听 ToolCallEndEvent 触发 AskUserQuestion"
```

---

## Task 3: 创建 AskUserQuestionBar 组件

**Files:**
- Create: `web/apps/gwsu-main/src/components/AIChat/AskUserQuestionBar.tsx`
- Create: `web/apps/gwsu-main/src/components/AIChat/AskUserQuestionBar.module.less`

- [ ] **Step 1: 创建样式文件 AskUserQuestionBar.module.less**

```less
/* AskUserQuestion 选择框 - 嵌入式展示在聊天输入框上方 */

.questionBar {
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: linear-gradient(
    135deg,
    oklch(97% 0.02 250) 0%,
    oklch(98% 0.01 250) 100%
  );
  border-top: 1px solid oklch(88% 0.04 250);
  border-bottom: 1px solid oklch(88% 0.04 250);
  animation: slideDown 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

/* 步骤头部：进度 + header标签 */
.stepHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.stepProgress {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary-color, #1a5fb4);
  background: oklch(95% 0.03 250);
  padding: 2px 8px;
  border-radius: 10px;
}

.stepHeaderLabel {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary-color, #6b7280);
}

/* 问题文本 */
.questionText {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color, #1a1a2e);
  line-height: 1.6;
  margin-bottom: 12px;
}

/* 选项列表 */
.optionsList {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

/* 单个选项 */
.optionItem {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1.5px solid oklch(92% 0.02 250);
  background: var(--surface-color, #ffffff);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    border-color: oklch(82% 0.06 250);
    background: oklch(98% 0.01 250);
  }
}

.optionItemSelected {
  border-color: var(--primary-color, #1a5fb4);
  background: oklch(97% 0.03 250);
  box-shadow: 0 0 0 1px var(--primary-color, #1a5fb4);
}

.optionRadio {
  margin-top: 2px;
  flex-shrink: 0;
}

.optionContent {
  flex: 1;
  min-width: 0;
}

.optionLabel {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color, #1a1a2e);
  line-height: 1.5;
}

.optionDescription {
  font-size: 12px;
  color: var(--text-secondary-color, #6b7280);
  line-height: 1.5;
  margin-top: 2px;
}

/* Other 输入区域 */
.otherInputArea {
  margin-top: 8px;
  padding-left: 32px;
  animation: slideDown 0.2s ease;
}

.otherInput {
  font-size: 13px !important;
}

/* 底部导航 */
.navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid oklch(92% 0.02 250);
}

.navButton {
  font-size: 13px !important;
  height: 32px !important;
  padding: 0 16px !important;
  border-radius: 8px !important;
}

.submitButton {
  font-size: 13px !important;
  height: 32px !important;
  padding: 0 20px !important;
  border-radius: 8px !important;
}

/* 步骤切换动画 */
.stepContent {
  animation: fadeIn 0.25s ease;
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

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
```

- [ ] **Step 2: 创建 AskUserQuestionBar.tsx 组件**

```tsx
import { Button, Input, Radio, Checkbox, QuestionCircleOutlined } from '@ant-design/icons';
import { useState, useEffect, useCallback, useRef } from 'react';
import { useAgent } from '@copilotkit/react-core/v2';
import { onAskUserQuestion, clearAskUserQuestion } from '@/services/ask-user-question';
import type { AskUserQuestionPayload, AskUserQuestionAnswer, QuestionParam } from '@/services/ask-user-question';
import styles from './AskUserQuestionBar.module.less';

/**
 * AskUserQuestion 嵌入式选择框
 * 分步式导航，一次展示一个问题，回答完自动跳转下一个
 * 与 HumanApprovalBar 平级，弹框活跃时输入框禁用
 */
export function AskUserQuestionBar() {
  const [payload, setPayload] = useState<AskUserQuestionPayload | null>(null);
  const [currentStep, setCurrentStep] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [annotations, setAnnotations] = useState<Record<string, { preview?: string; notes?: string }>>({});
  const [otherInputs, setOtherInputs] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const { agent } = useAgent({ agentId: 'brain' });
  const autoAdvanceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 监听 AskUserQuestion 事件
  useEffect(() => {
    const unsubscribe = onAskUserQuestion((p) => {
      // 清除可能存在的自动跳转定时器
      if (autoAdvanceTimer.current) {
        clearTimeout(autoAdvanceTimer.current);
        autoAdvanceTimer.current = null;
      }
      setPayload(p);
      setCurrentStep(0);
      setAnswers({});
      setAnnotations({});
      setOtherInputs({});
    });
    return unsubscribe;
  }, []);

  // 组件卸载时清除定时器
  useEffect(() => {
    return () => {
      if (autoAdvanceTimer.current) {
        clearTimeout(autoAdvanceTimer.current);
      }
    };
  }, []);

  // 提交答案
  const submitAnswer = useCallback(async () => {
    if (!payload) return;
    setSubmitting(true);
    try {
      const answer: AskUserQuestionAnswer = { answers, annotations };
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
  }, [payload, answers, annotations, agent]);

  // 选中选项
  const handleSelect = useCallback((question: QuestionParam, label: string, isOther: boolean) => {
    const key = question.question;

    if (question.multiSelect) {
      // 多选：切换选中状态
      const current = answers[key] ? answers[key].split(', ') : [];
      const isSelected = current.includes(label);

      let newSelected: string[];
      if (isSelected) {
        newSelected = current.filter((s) => s !== label);
        // 如果取消选中 Other，清除 Other 输入
        if (isOther) {
          setOtherInputs((prev) => {
            const next = { ...prev };
            delete next[key];
            return next;
          });
          setAnnotations((prev) => {
            const next = { ...prev };
            delete next[key];
            return next;
          });
        }
      } else {
        newSelected = [...current, label];
      }
      setAnswers((prev) => ({ ...prev, [key]: newSelected.join(', ') }));
    } else {
      // 单选
      setAnswers((prev) => ({ ...prev, [key]: isOther ? '__other__' : label }));

      if (isOther) {
        // Other 选项不自动跳转，等待用户输入
        return;
      }

      // 单选且非 Other：自动跳转到下一个问题
      if (payload && currentStep < payload.questions.length - 1) {
        if (autoAdvanceTimer.current) {
          clearTimeout(autoAdvanceTimer.current);
        }
        autoAdvanceTimer.current = setTimeout(() => {
          setCurrentStep((prev) => prev + 1);
          autoAdvanceTimer.current = null;
        }, 300);
      }
    }
  }, [answers, payload, currentStep]);

  // 处理 Other 输入变化
  const handleOtherInputChange = useCallback((questionKey: string, value: string) => {
    setOtherInputs((prev) => ({ ...prev, [questionKey]: value }));
  }, []);

  // Other 输入确认（按 Enter 或失焦时更新答案）
  const handleOtherInputConfirm = useCallback((questionKey: string) => {
    const text = otherInputs[questionKey]?.trim();
    if (!text) return;

    const question = payload?.questions.find((q) => q.question === questionKey);
    if (!question) return;

    if (question.multiSelect) {
      // 多选模式下，Other 文本作为额外选项追加
      const current = answers[questionKey] ? answers[questionKey].split(', ') : [];
      const newSelected = current.filter((s) => s !== 'Other');
      newSelected.push(text);
      setAnswers((prev) => ({ ...prev, [questionKey]: newSelected.join(', ') }));
    } else {
      // 单选模式，直接使用输入文本
      setAnswers((prev) => ({ ...prev, [questionKey]: text }));
    }

    // 记录到 annotations
    setAnnotations((prev) => ({
      ...prev,
      [questionKey]: { notes: text },
    }));
  }, [otherInputs, answers, payload]);

  // 上一步
  const handlePrev = useCallback(() => {
    if (currentStep > 0) {
      setCurrentStep((prev) => prev - 1);
    }
  }, [currentStep]);

  // 下一步
  const handleNext = useCallback(() => {
    if (payload && currentStep < payload.questions.length - 1) {
      setCurrentStep((prev) => prev + 1);
    }
  }, [payload, currentStep]);

  // 无待回答事件时不渲染
  if (!payload || payload.questions.length === 0) return null;

  const questions = payload.questions;
  const currentQuestion = questions[currentStep];
  const isLastStep = currentStep === questions.length - 1;
  const isFirstStep = currentStep === 0;
  const totalSteps = questions.length;

  // 当前问题是否已作答
  const currentAnswer = answers[currentQuestion.question];
  const isCurrentAnswered = Boolean(currentAnswer && currentAnswer !== '__other__')
    || (currentAnswer === '__other__' && Boolean(otherInputs[currentQuestion.question]?.trim()));

  // 所有问题是否都已作答
  const allAnswered = questions.every((q) => {
    const ans = answers[q.question];
    return ans && ans !== '__other__' || (ans === '__other__' && Boolean(otherInputs[q.question]?.trim()));
  });

  // 获取当前问题的选中值
  const selectedValues = (() => {
    const ans = currentAnswer;
    if (!ans) return [];
    if (ans === '__other__') return ['Other'];
    return ans.split(', ');
  })();

  return (
    <div className={styles.questionBar}>
      <div className={styles.stepContent} key={currentStep}>
        {/* 步骤头部 */}
        <div className={styles.stepHeader}>
          <div className={styles.stepHeaderLabel}>
            <QuestionCircleOutlined />
            <span>{currentQuestion.header}</span>
          </div>
          <span className={styles.stepProgress}>{currentStep + 1}/{totalSteps}</span>
        </div>

        {/* 问题文本 */}
        <div className={styles.questionText}>{currentQuestion.question}</div>

        {/* 选项列表 */}
        <div className={styles.optionsList}>
          {currentQuestion.options.map((option) => {
            const isSelected = selectedValues.includes(option.label);
            return (
              <div
                key={option.label}
                className={`${styles.optionItem} ${isSelected ? styles.optionItemSelected : ''}`}
                onClick={() => handleSelect(currentQuestion, option.label, false)}
              >
                <div className={styles.optionRadio}>
                  {currentQuestion.multiSelect ? (
                    <Checkbox checked={isSelected} />
                  ) : (
                    <Radio checked={isSelected} />
                  )}
                </div>
                <div className={styles.optionContent}>
                  <div className={styles.optionLabel}>{option.label}</div>
                  {option.description && (
                    <div className={styles.optionDescription}>{option.description}</div>
                  )}
                </div>
              </div>
            );
          })}

          {/* Other 选项 */}
          <div
            className={`${styles.optionItem} ${selectedValues.includes('Other') ? styles.optionItemSelected : ''}`}
            onClick={() => handleSelect(currentQuestion, 'Other', true)}
          >
            <div className={styles.optionRadio}>
              {currentQuestion.multiSelect ? (
                <Checkbox checked={selectedValues.includes('Other')} />
              ) : (
                <Radio checked={selectedValues.includes('Other')} />
              )}
            </div>
            <div className={styles.optionContent}>
              <div className={styles.optionLabel}>Other</div>
            </div>
          </div>

          {/* Other 输入框 */}
          {selectedValues.includes('Other') && (
            <div className={styles.otherInputArea}>
              <Input.TextArea
                className={styles.otherInput}
                value={otherInputs[currentQuestion.question] || ''}
                onChange={(e) => handleOtherInputChange(currentQuestion.question, e.target.value)}
                onBlur={() => handleOtherInputConfirm(currentQuestion.question)}
                onPressEnter={(e) => {
                  if (!e.shiftKey) {
                    e.preventDefault();
                    handleOtherInputConfirm(currentQuestion.question);
                  }
                }}
                placeholder="请输入..."
                autoSize={{ minRows: 1, maxRows: 3 }}
                maxLength={500}
              />
            </div>
          )}
        </div>
      </div>

      {/* 底部导航 */}
      <div className={styles.navigation}>
        <Button
          className={styles.navButton}
          disabled={isFirstStep}
          onClick={handlePrev}
        >
          上一个
        </Button>
        {isLastStep ? (
          <Button
            type="primary"
            className={styles.submitButton}
            disabled={!allAnswered || submitting}
            loading={submitting}
            onClick={submitAnswer}
          >
            提交答案
          </Button>
        ) : (
          <Button
            type="primary"
            className={styles.navButton}
            disabled={!isCurrentAnswered}
            onClick={handleNext}
          >
            下一个
          </Button>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 验证前端编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web/apps/gwsu-main && npx tsc --noEmit 2>&1 | head -20`
Expected: 无类型错误

- [ ] **Step 4: 提交**

```bash
git add web/apps/gwsu-main/src/components/AIChat/AskUserQuestionBar.tsx
git add web/apps/gwsu-main/src/components/AIChat/AskUserQuestionBar.module.less
git commit -m "feat(ask-user-question): 新增分步式选择框组件 AskUserQuestionBar"
```

---

## Task 4: 集成到 CopilotChatPanel + 互斥控制 + 历史恢复

**Files:**
- Modify: `web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx`
- Modify: `web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less`

- [ ] **Step 1: 在 CopilotChatPanel.tsx 中添加 import**

在文件顶部 import 区域添加：

```ts
import { AskUserQuestionBar } from './AskUserQuestionBar';
import { onAskUserQuestion, clearAskUserQuestion } from '@/services/ask-user-question';
```

- [ ] **Step 2: 在 CopilotChatPanel 组件中添加互斥控制状态**

在 `CopilotChatPanel` 函数体内部，现有 `useState` 声明之后添加：

```ts
// 弹框与输入框互斥控制
const [hasApproval, setHasApproval] = useState(false);
const [hasAskQuestion, setHasAskQuestion] = useState(false);

useEffect(() => {
  const unsubApproval = onHumanApproval((payload) => setHasApproval(payload !== null));
  const unsubAskQuestion = onAskUserQuestion((payload) => setHasAskQuestion(payload !== null));
  return () => {
    unsubApproval();
    unsubAskQuestion();
  };
}, []);

const isInteractionActive = hasApproval || hasAskQuestion;
```

- [ ] **Step 3: 在 handleNewSession 中清除 AskUserQuestion 状态**

在 `handleNewSession` 函数中，`clearHumanApproval()` 之后添加：

```ts
clearAskUserQuestion();
```

- [ ] **Step 4: 在 handleLoadSession 中清除旧状态并恢复 AskUserQuestion**

在 `handleLoadSession` 函数中，`clearHumanApproval()` 之后添加 `clearAskUserQuestion()`：

```ts
clearHumanApproval();
clearAskUserQuestion();
```

在 `agent.setMessages(formattedMessages)` 之后、审批状态检查之前，添加 AskUserQuestion 历史恢复逻辑：

```ts
// 检查是否需要恢复 AskUserQuestion 弹框
// 最新一条消息如果是 AskUserQuestion 工具调用且无对应结果，则恢复弹框
try {
  const lastMsg = messages[messages.length - 1] as BrainMessage | undefined;
  if (lastMsg?.role === 'assistant' && lastMsg.toolCalls?.length) {
    const askQuestionToolCall = lastMsg.toolCalls.find(
      (tc: Record<string, unknown>) => tc.name === 'AskUserQuestion' || tc.function?.name === 'AskUserQuestion'
    );
    if (askQuestionToolCall) {
      // 从 toolCall 参数中提取 questions
      const args = typeof askQuestionToolCall.function?.arguments === 'string'
        ? JSON.parse(askQuestionToolCall.function.arguments)
        : askQuestionToolCall.function?.arguments ?? askQuestionToolCall.args ?? {};
      dispatchAskUserQuestion({
        toolCallId: askQuestionToolCall.id as string,
        questions: Array.isArray(args.questions) ? args.questions.map((q: Record<string, unknown>) => ({
          question: String(q.question ?? ''),
          header: String(q.header ?? ''),
          options: Array.isArray(q.options) ? q.options : (q.options ? [q.options] : []),
          multiSelect: Boolean(q.multiSelect),
        })) : [],
      });
    }
  }
} catch (e) {
  console.warn('[AskUserQuestion] 历史会话恢复失败:', e);
}
```

需要额外添加 import：

```ts
import { dispatchAskUserQuestion } from '@/services/ask-user-question';
```

> 注意：`onHumanApproval` 已在现有代码中 import，无需重复添加。`dispatchAskUserQuestion` 和 `onAskUserQuestion`、`clearAskUserQuestion` 需要添加 import。

- [ ] **Step 5: 在 renderChatContent 中添加 AskUserQuestionBar 和互斥 CSS 类**

在 `renderChatContent` 方法中，`<HumanApprovalBar />` 之后添加 `<AskUserQuestionBar />`：

```tsx
{/* 人工审批卡片 - 展示在聊天输入框上方 */}
<HumanApprovalBar />
{/* AskUserQuestion 选择框 - 展示在审批卡片下方 */}
<AskUserQuestionBar />
```

同时，给 `copilotChatWrapper` 的 div 添加互斥 CSS 类：

在 `renderChatContent` 返回的 JSX 中，找到 `copilotChatWrapper` div 的 className，添加互斥条件类：

```tsx
className={`${styles.copilotChatWrapper} ${isHidden ? styles.hiddenWrapper : ''} ${isDraggableMode ? styles.draggableWrapper : ''} ${isInteractionActive ? styles.interactionActive : ''} ${className || ''}`}
```

- [ ] **Step 6: 在 copilot-override.module.less 中添加互斥禁用样式**

在 `copilot-override.module.less` 文件末尾添加：

```less
/* 互斥控制：弹框活跃时禁用输入区域 */
.interactionActive {
  :global(.copilot-chat-input-container) {
    pointer-events: none;
    opacity: 0.4;
    filter: grayscale(0.3);
    transition: opacity 0.2s ease, filter 0.2s ease;
  }
}
```

- [ ] **Step 7: 验证前端编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web/apps/gwsu-main && npx tsc --noEmit 2>&1 | head -20`
Expected: 无类型错误

- [ ] **Step 8: 提交**

```bash
git add web/apps/gwsu-main/src/components/AIChat/CopilotChatPanel.tsx
git add web/apps/gwsu-main/src/components/AIChat/copilot-override.module.less
git commit -m "feat(ask-user-question): 集成 AskUserQuestionBar + 互斥控制 + 历史恢复"
```

---

## Task 5: 端到端验证

- [ ] **Step 1: 前端全量编译**

Run: `cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm build:core && pnpm build:main 2>&1 | tail -10`
Expected: 编译成功

- [ ] **Step 2: 启动前端服务，手动验证 AskUserQuestion 流程**

1. 启动前端：`cd /Users/quyq/Documents/work/personal/gwsu-basic/web && pnpm dev:main`
2. 在智能助手中输入能触发 AskUserQuestion 工具的对话
3. 验证：SSE 流结束后，聊天输入框上方出现 AskUserQuestion 选择框
4. 验证：选择框展示时，输入框被禁用（灰色、不可点击）
5. 选中一个选项后，验证自动跳转到下一个问题
6. 验证"上一个"/"下一个"按钮导航正常
7. 所有问题回答后，点击"提交答案"
8. 验证：Agent 恢复执行，选择框消失，输入框恢复可用
9. 验证 HumanApprovalBar 弹出时，输入框同样被禁用

- [ ] **Step 3: 验证历史会话恢复**

1. 触发 AskUserQuestion 后不要作答
2. 点击历史记录，加载另一个会话，再切回来
3. 验证：AskUserQuestion 选择框恢复展示

- [ ] **Step 4: 最终提交**

如有修复：

```bash
git add -A
git commit -m "fix(ask-user-question): 端到端验证修复"
```
