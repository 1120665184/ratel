import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import { useOperationTabStore } from '@/stores/operationTab';

/**
 * 进入AI操作模式工具
 * 将操作模式切换为AI模式，锁定界面控制权
 * 同时切换到界面Tab，让用户能看到AI操作界面
 */
const enterAiModeTool: WebToolExecutor = {
  async execute(): Promise<WebToolResult> {
    useForwardedPropsStore.getState().setOperationMode('ai');
    useOperationTabStore.getState().switchToInterface();
    return { success: true, result: '已进入AI操作模式，界面控制权已交给智能助手' };
  },
};

registerWebTool('EnterAiMode', enterAiModeTool);
