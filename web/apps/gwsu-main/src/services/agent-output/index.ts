export type { AgentOutputPayload } from './types';

import type { AgentOutputPayload } from './types';

/** 输出事件监听器列表 */
const outputListeners = new Set<(payload: AgentOutputPayload) => void>();

/** 当前展示的 spec 内容 */
let currentSpec: string | null = null;

/**
 * 分发 AI 输出事件
 * 由 CopilotKitProvider 中的 CUSTOM 事件监听调用
 */
export function dispatchAgentOutput(payload: AgentOutputPayload): void {
  currentSpec = payload.text;
  outputListeners.forEach((listener) => listener(payload));
}

/**
 * 清除当前输出内容
 */
export function clearAgentOutput(): void {
  currentSpec = null;
  outputListeners.forEach((listener) => listener({ text: '' }));
}

/**
 * 获取当前输出内容
 */
export function getCurrentOutput(): string | null {
  return currentSpec;
}

/**
 * 注册输出事件监听器（供 AiOutputPanel 使用）
 * @returns 取消监听的函数
 */
export function onAgentOutput(listener: (payload: AgentOutputPayload) => void): () => void {
  outputListeners.add(listener);
  return () => {
    outputListeners.delete(listener);
  };
}
