/**
 * 高亮标注管理
 * 为可交互元素添加可视化高亮标注，帮助开发调试
 */

/** 高亮覆盖层元素列表 */
let highlightElements: HTMLElement[] = [];

/** 是否启用高亮（默认关闭） */
let highlightEnabled = false;

/**
 * 设置是否启用高亮
 */
export function setHighlightEnabled(enabled: boolean): void {
  highlightEnabled = enabled;
  if (!enabled) {
    clearHighlights();
  }
}

/**
 * 清除所有高亮标注
 */
export function clearHighlights(): void {
  for (const el of highlightElements) {
    el.remove();
  }
  highlightElements = [];
}

/**
 * 为元素创建高亮标注
 * @param index 元素索引
 * @param element 目标DOM元素
 */
export function showHighlight(index: number, element: Element): void {
  if (!highlightEnabled) return;

  const rect = element.getBoundingClientRect();

  // 创建索引标签
  const label = document.createElement('div');
  label.className = 'ai-highlight-label';
  label.textContent = String(index);
  Object.assign(label.style, {
    position: 'fixed',
    left: `${rect.left}px`,
    top: `${rect.top}px`,
    zIndex: '99999',
    background: 'rgba(255, 0, 0, 0.8)',
    color: 'white',
    fontSize: '10px',
    fontWeight: 'bold',
    padding: '1px 4px',
    borderRadius: '2px',
    pointerEvents: 'none',
    fontFamily: 'monospace',
  });

  document.body.appendChild(label);
  highlightElements.push(label);
}

/**
 * 批量显示高亮（遍历selectorMap中的所有元素）
 */
export function showHighlights(selectorMap: { getElement: (index: number) => Element | undefined; size: number }): void {
  if (!highlightEnabled) return;

  clearHighlights();

  for (let i = 0; i < selectorMap.size; i++) {
    const element = selectorMap.getElement(i);
    if (element) {
      showHighlight(i, element);
    }
  }
}
