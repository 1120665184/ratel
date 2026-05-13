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
