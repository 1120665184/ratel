import { post } from '@gwsu/core';
import { getWebTool } from './registry';
import type { WebToolExecutePayload, WebToolCallbackRequest, WebToolConfirmEvent } from './types';
import { useForwardedPropsStore } from '@/stores/forwardedProps';

/** 需要 AI 操作模式的工具列表 */
const AI_MODE_REQUIRED_TOOLS = [
  'RouteNavigation',
  'ClickElement',
  'InputText',
  'SelectOption',
  'ScrollPage',
];

/** 确认事件监听器列表 */
const confirmListeners = new Set<(event: WebToolConfirmEvent) => void>();

/**
 * 注册确认事件监听器（供 UI 组件使用）
 */
export function onWebToolConfirm(listener: (event: WebToolConfirmEvent) => void): () => void {
  confirmListeners.add(listener);
  return () => {
    confirmListeners.delete(listener);
  };
}

/**
 * 分发Web工具执行
 * 通用流程：查找执行器 → (INTERACTIVE时用户确认) → 执行工具 → 回调结果
 * 只有"执行工具"步骤调用不同的executor，其余均为通用逻辑
 */
export async function dispatchWebTool(payload: WebToolExecutePayload): Promise<void> {
  const { toolCallId, toolName, params } = payload;

  // 操作模式守卫：需要AI模式的工具在人类模式下直接拒绝
  const mode = useForwardedPropsStore.getState().operationMode;
  if (mode === 'human' && AI_MODE_REQUIRED_TOOLS.includes(toolName)) {
    await callbackToolResult(toolCallId, false, '当前为人类操作模式，请先进入AI操作模式');
    return;
  }

  const executor = getWebTool(toolName);

  if (!executor) {
    await callbackToolResult(toolCallId, false, `未知工具: ${toolName}`);
    return;
  }

  try {
    const result = await executor.execute(params);
    await callbackToolResult(toolCallId, result.success, result.result);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    await callbackToolResult(toolCallId, false, `执行异常: ${message}`);
  }
}

/**
 * 回调工具执行结果给后端
 * 通用逻辑，所有工具执行后统一调用
 */
async function callbackToolResult(
  toolCallId: string,
  success: boolean,
  result: string,
): Promise<void> {
  try {
    await post<unknown>('/security/brain/tool/callback', {
      toolCallId,
      success,
      result,
    } satisfies WebToolCallbackRequest);
  } catch (error) {
    console.error('[WebTool] 回调结果失败:', error);
  }
}
