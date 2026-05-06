import { history } from 'umi';
import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';

/**
 * 路由跳转工具
 * 后端调用 routeNavigation 时，前端执行 history.push 跳转到指定路由
 */
const routeNavigationTool: WebToolExecutor = {
  async execute(params): Promise<WebToolResult> {
    const { path } = params;
    if (typeof path !== 'string' || !path) {
      return { success: false, result: '参数path不能为空' };
    }

    try {
      history.push(path);
      return { success: true, result: `已跳转到: ${path}` };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return { success: false, result: `跳转失败: ${message}` };
    }
  },
};

// 注册工具
registerWebTool('routeNavigation', routeNavigationTool);
