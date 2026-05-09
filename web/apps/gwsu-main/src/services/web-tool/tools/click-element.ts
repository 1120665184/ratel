import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import { selectorMap, isElementVisible, sleep, elementDescription } from '../dom-engine';

/**
 * 点击元素工具
 * 通过元素索引点击界面上的元素，执行完整的W3C指针事件序列
 */
const clickElementTool: WebToolExecutor = {
  async execute(params): Promise<WebToolResult> {
    const index = Number(params.index);
    if (isNaN(index)) {
      return { success: false, result: '参数index必须是数字' };
    }

    const element = selectorMap.getElement(index);
    if (!element) {
      return {
        success: false,
        result: `未找到索引为 ${index} 的元素，请先调用GetPageState刷新界面状态`,
      };
    }

    // 检查元素是否仍然可见
    if (!isElementVisible(element)) {
      return {
        success: false,
        result: `索引 ${index} 的元素已不可见，请重新调用GetPageState`,
      };
    }

    try {
      // 滚动元素到可见区域
      if (element instanceof HTMLElement) {
        // @ts-ignore
        element.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' });
      }
      await sleep(100);

      // 计算点击坐标
      const rect = element.getBoundingClientRect();
      const x = rect.left + rect.width / 2;
      const y = rect.top + rect.height / 2;

      const pointerOpts: PointerEventInit = {
        bubbles: true,
        cancelable: true,
        clientX: x,
        clientY: y,
        pointerType: 'mouse',
      };
      const mouseOpts: MouseEventInit = {
        bubbles: true,
        cancelable: true,
        clientX: x,
        clientY: y,
        button: 0,
      };

      // W3C 指针事件序列
      // Hover
      element.dispatchEvent(new PointerEvent('pointerover', pointerOpts));
      element.dispatchEvent(new PointerEvent('pointerenter', { ...pointerOpts, bubbles: false }));
      element.dispatchEvent(new MouseEvent('mouseover', mouseOpts));
      element.dispatchEvent(new MouseEvent('mouseenter', { ...mouseOpts, bubbles: false }));

      // Press
      element.dispatchEvent(new PointerEvent('pointerdown', pointerOpts));
      element.dispatchEvent(new MouseEvent('mousedown', mouseOpts));

      // Focus
      if (element instanceof HTMLElement) {
        element.focus({ preventScroll: true });
      }

      // Release
      element.dispatchEvent(new PointerEvent('pointerup', pointerOpts));
      element.dispatchEvent(new MouseEvent('mouseup', mouseOpts));

      // Click
      element.dispatchEvent(new MouseEvent('click', mouseOpts));

      // 等待UI响应
      await sleep(300);

      return {
        success: true,
        result: `已点击索引 ${index} 的元素: ${elementDescription(element)}`,
      };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return { success: false, result: `点击失败: ${message}` };
    }
  },
};

// 注册工具
registerWebTool('ClickElement', clickElementTool);
