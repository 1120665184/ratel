/**
 * 元素索引管理器
 * 管理 highlightIndex 到 DOM 元素的映射，每次 GetPageState 调用时重建
 */
export class SelectorMap {
  /** 索引 -> DOM元素映射 */
  private map = new Map<number, Element>();

  /** 当前最大索引 */
  private nextIndex = 0;

  /**
   * 注册一个可交互元素，分配唯一索引
   * @returns 分配的索引值
   */
  register(element: Element): number {
    const index = this.nextIndex++;
    this.map.set(index, element);
    return index;
  }

  /**
   * 根据索引获取DOM元素
   * @returns 元素引用，不存在或已从DOM移除则返回 undefined
   */
  getElement(index: number): Element | undefined {
    const element = this.map.get(index);
    if (!element) return undefined;

    // 检查元素是否仍在DOM中
    if (!document.body.contains(element)) {
      this.map.delete(index);
      return undefined;
    }

    return element;
  }

  /** 当前注册的元素数量 */
  get size(): number {
    return this.map.size;
  }

  /** 清空并重置索引（每次 GetPageState 调用时执行） */
  reset(): void {
    this.map.clear();
    this.nextIndex = 0;
  }
}
