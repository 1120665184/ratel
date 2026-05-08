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
