/**
 * DOM 通用工具函数
 */

/** 等待指定毫秒数 */
export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** 需要跳过的标签名集合 */
const SKIP_TAGS = new Set([
  'script',
  'style',
  'link',
  'meta',
  'noscript',
  'template',
  'head',
  'br',
  'hr',
  'wbr',
]);

/** 判断元素是否需要跳过（脚本、样式等非内容节点） */
export function shouldSkipTag(tagName: string): boolean {
  return SKIP_TAGS.has(tagName);
}

/** 判断元素是否可见 */
export function isElementVisible(element: Element): boolean {
  if (!(element instanceof HTMLElement)) {
    return true;
  }

  const style = window.getComputedStyle(element);
  if (style.display === 'none') return false;
  if (style.visibility === 'hidden') return false;
  if (style.opacity === '0') return false;

  // 零尺寸元素不可见（但允许隐藏输入框等）
  const rect = element.getBoundingClientRect();
  if (rect.width === 0 && rect.height === 0) {
    // 排除隐藏类型的input
    if (element.tagName === 'INPUT' && (element as HTMLInputElement).type === 'hidden') {
      return false;
    }
    // 排除零尺寸的容器div
    return false;
  }

  return true;
}

/** 判断文本节点是否可见 */
export function isTextNodeVisible(textNode: Text): boolean {
  const parent = textNode.parentElement;
  if (!parent) return false;
  return isElementVisible(parent);
}

/** 截断文本 */
export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}

/** 获取元素的简短描述（用于工具结果信息） */
export function elementDescription(element: Element): string {
  const tag = element.tagName.toLowerCase();
  const id = element.id ? `#${element.id}` : '';
  const className =
    element.className && typeof element.className === 'string'
      ? `.${element.className.split(/\s+/).slice(0, 2).join('.')}`
      : '';
  const text = element.textContent?.trim().substring(0, 30) || '';
  return `<${tag}${id}${className}>${text ? ` "${text}"` : ''}`;
}

/** 轮询等待条件满足 */
export async function waitForCondition(
  condition: () => boolean,
  options: { interval?: number; timeout?: number } = {},
): Promise<boolean> {
  const { interval = 100, timeout = 2000 } = options;
  const start = Date.now();

  while (!condition()) {
    if (Date.now() - start >= timeout) {
      return false;
    }
    await sleep(interval);
  }
  return true;
}
