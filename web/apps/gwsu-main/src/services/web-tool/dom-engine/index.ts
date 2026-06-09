/**
 * DOM 提取引擎入口
 * 统一导出公共 API
 */

import { SelectorMap } from './selector-map';

export { SelectorMap } from './selector-map';
export { buildDomTree, isInteractiveElement, type DomNode } from './dom-builder';
export { flatTreeToString, getPageInfo, type PageInfo } from './dom-serializer';
export { resolveScope, isExcluded, isInScope, type ScopeResult } from './scope-resolver';
export {
  clearHighlights,
  showHighlights,
  setHighlightEnabled,
} from './highlight';
export {
  isElementVisible,
  shouldSkipTag,
  sleep,
  truncateText,
  elementDescription,
  waitForCondition,
  applyHoverState,
  clearHoverState,
} from './dom-utils';

/** 全局 SelectorMap 单例，各操作工具共享 */
export const selectorMap = new SelectorMap();
