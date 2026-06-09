import { registerWebTool } from '../registry';
import type { WebToolExecutor, WebToolResult } from '../types';
import { selectorMap, isElementVisible, sleep, elementDescription, applyHoverState } from '../dom-engine';

const HoverElementTool: WebToolExecutor = {
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

    if (!isElementVisible(element)) {
      return {
        success: false,
        result: `索引 ${index} 的元素已不可见，请重新调用GetPageState`,
      };
    }

    try {
      if (element instanceof HTMLElement) {
        element.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' });
      }
      await sleep(100);

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

      element.dispatchEvent(new PointerEvent('pointerover', pointerOpts));
      element.dispatchEvent(new PointerEvent('pointerenter', { ...pointerOpts, bubbles: false }));
      element.dispatchEvent(new MouseEvent('mouseover', mouseOpts));
      element.dispatchEvent(new MouseEvent('mouseenter', { ...mouseOpts, bubbles: false }));

      element.dispatchEvent(new PointerEvent('pointermove', { ...pointerOpts, clientX: x + 1, clientY: y + 1 }));
      element.dispatchEvent(new MouseEvent('mousemove', { ...mouseOpts, clientX: x + 1, clientY: y + 1 }));

      applyHoverState(element);

      await sleep(300);

      return {
        success: true,
        result: `已悬停索引 ${index} 的元素: ${elementDescription(element)}，悬停后可能出现新的交互元素，请调用GetPageState查看`,
      };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return { success: false, result: `悬停失败: ${message}` };
    }
  },
};

registerWebTool('HoverElement', HoverElementTool);
