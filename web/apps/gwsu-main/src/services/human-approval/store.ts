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
