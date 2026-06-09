import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import { useForwardedPropsStore } from '@/stores/forwardedProps';
import { clearHoverState } from '../dom-engine';

const exitAiModeTool: WebToolExecutor = {
  async execute(): Promise<WebToolResult> {
    clearHoverState();
    useForwardedPropsStore.getState().setOperationMode('human');
    return { success: true, result: '已退出AI操作模式，界面控制权已交还给用户' };
  },
};

registerWebTool('ExitAiMode', exitAiModeTool);
