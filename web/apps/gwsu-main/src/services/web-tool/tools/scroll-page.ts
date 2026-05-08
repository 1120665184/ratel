import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import { sleep, getPageInfo } from '../dom-engine';

/**
 * 滚动页面工具
 * 滚动页面或页面内的可滚动元素
 */
const scrollPageTool: WebToolExecutor = {
  async execute(params): Promise<WebToolResult> {
    const direction = String(params.direction ?? 'down').toLowerCase();
    const amount = Number(params.amount ?? 1);

    if (isNaN(amount) || amount <= 0) {
      return { success: false, result: '参数amount必须是正数' };
    }

    const validDirections = ['up', 'down', 'left', 'right'];
    if (!validDirections.includes(direction)) {
      return {
        success: false,
        result: `不支持的方向: ${direction}，请使用 ${validDirections.join('/')}`,
      };
    }

    try {
      // 查找滚动容器
      const scrollContainer = findScrollContainer();

      // 计算滚动距离
      const containerHeight = scrollContainer?.clientHeight || window.innerHeight;
      const containerWidth = scrollContainer?.clientWidth || window.innerWidth;
      // amount < 100 视为页数倍率，>= 100 视为像素值
      const isPageUnit = amount < 100;
      const scrollStep = isPageUnit
        ? (direction === 'up' || direction === 'down' ? containerHeight : containerWidth) * amount
        : amount;

      let scrollX = 0;
      let scrollY = 0;

      switch (direction) {
        case 'down':
          scrollY = scrollStep;
          break;
        case 'up':
          scrollY = -scrollStep;
          break;
        case 'right':
          scrollX = scrollStep;
          break;
        case 'left':
          scrollX = -scrollStep;
          break;
      }

      if (scrollContainer) {
        scrollContainer.scrollBy({ top: scrollY, left: scrollX, behavior: 'smooth' });
      } else {
        window.scrollBy({ top: scrollY, left: scrollX, behavior: 'smooth' });
      }

      await sleep(400);
      const pageInfo = getPageInfo();

      const directionLabel = { up: '上', down: '下', left: '左', right: '右' }[direction];
      const amountLabel = isPageUnit ? `${amount}页` : `${amount}px`;

      return {
        success: true,
        result: `已向${directionLabel}滚动${amountLabel}，当前滚动位置: x=${pageInfo.scrollX}, y=${pageInfo.scrollY}`,
      };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return { success: false, result: `滚动失败: ${message}` };
    }
  },
};

/**
 * 查找主内容滚动容器
 * 优先查找 InterfaceOperation 的 operationContent 区域
 */
function findScrollContainer(): HTMLElement | null {
  // 通过 data-ai-scope 标记找到 InterfaceOperation 根，再查找其内部滚动容器
  const scopeRoot = document.querySelector('[data-ai-scope="interface-operation"]');
  if (scopeRoot) {
    // 查找 operationContent 样式的容器（overflow:auto 的滚动容器）
    const contentArea = scopeRoot.querySelector('[class*="operationContent"]');
    if (contentArea instanceof HTMLElement) {
      return contentArea;
    }
    // 备选：查找 microAppContainer
    const microApp = scopeRoot.querySelector('[class*="microAppContainer"]');
    if (microApp instanceof HTMLElement) {
      return microApp;
    }
  }

  // 降级：查找页面上可见的滚动容器
  const candidates = document.querySelectorAll('*');
  for (const el of candidates) {
    if (el instanceof HTMLElement) {
      const style = window.getComputedStyle(el);
      const overflowY = style.overflowY;
      if (
        (overflowY === 'auto' || overflowY === 'scroll') &&
        el.scrollHeight > el.clientHeight &&
        el.clientHeight >= window.innerHeight * 0.5
      ) {
        return el;
      }
    }
  }

  return null;
}

// 注册工具
registerWebTool('ScrollPage', scrollPageTool);
