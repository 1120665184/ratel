/**
 * DOM 范围解析器
 * 确定 DOM 提取的根范围，同时处理弹框/抽屉检测
 */

/** DOM 范围解析结果 */
export interface ScopeResult {
  /** 主内容根节点（InterfaceOperation） */
  mainRoot: Element | null;
  /** 可见弹框/抽屉根节点列表 */
  modalRoots: Element[];
}

/** AI 范围标识属性 */
const AI_SCOPE_ATTR = 'data-ai-scope';
const AI_EXCLUDE_ATTR = 'data-ai-exclude';

/** 主内容区域的 scope 值 */
const MAIN_SCOPE_VALUE = 'interface-operation';

/**
 * 解析 DOM 提取范围
 * 获取主内容区域根节点 + 可见弹框/抽屉/下拉菜单节点
 */
export function resolveScope(): ScopeResult {
  // 1. 查找主内容根
  const mainRoot = document.querySelector(`[${AI_SCOPE_ATTR}="${MAIN_SCOPE_VALUE}"]`);

  // 2. 查找可见的 portal 组件（Modal/Drawer/Dropdown 等）
  const modalRoots: Element[] = [];

  // Ant Design Modal
  collectVisibleModals(modalRoots);

  // Ant Design Drawer
  collectVisibleDrawers(modalRoots);

  // Ant Design Dropdown / Menu 下拉菜单
  collectVisibleDropdowns(modalRoots);

  // Ant Design Select / Cascader / DatePicker 下拉面板
  collectVisibleSelectDropdowns(modalRoots);

  // Ant Design Popover / Popconfirm
  collectVisiblePopovers(modalRoots);

  // 排除标记了 data-ai-exclude 的元素
  const filteredRoots = modalRoots.filter((el) => !el.hasAttribute(AI_EXCLUDE_ATTR));

  return { mainRoot, modalRoots: filteredRoots };
}

/**
 * 收集可见的 Modal
 */
function collectVisibleModals(results: Element[]): void {
  const modals = document.querySelectorAll('.ant-modal-wrap');
  modals.forEach((el) => {
    if (isVisible(el) && !results.includes(el)) {
      results.push(el);
    }
  });
}

/**
 * 收集可见的 Drawer
 * Ant Design 6 Drawer DOM 结构:
 *   .ant-drawer.ant-drawer-open
 *     .ant-drawer-mask
 *     .ant-drawer-content-wrapper
 *       .ant-drawer-content
 */
function collectVisibleDrawers(results: Element[]): void {
  // 通过 .ant-drawer-open 类判断 Drawer 是否打开
  const drawers = document.querySelectorAll('.ant-drawer');
  drawers.forEach((el) => {
    // 方式1: 检查 .ant-drawer-open 类
    if (el.classList.contains('ant-drawer-open')) {
      if (!results.includes(el)) {
        results.push(el);
      }
      return;
    }

    // 方式2: 降级检查可见性（某些动画中间状态）
    if (isVisible(el) && !results.includes(el)) {
      results.push(el);
    }
  });
}

/**
 * 收集可见的 Popover/Popconfirm
 */
function collectVisiblePopovers(results: Element[]): void {
  const selectors = ['.ant-popover:not(.ant-popover-hidden)', '.ant-popconfirm:not(.ant-popconfirm-hidden)'];
  for (const selector of selectors) {
    try {
      const elements = document.querySelectorAll(selector);
      elements.forEach((el) => {
        if (isVisible(el) && !results.includes(el)) {
          results.push(el);
        }
      });
    } catch {
      // 选择器不兼容，忽略
    }
  }
}

/**
 * 收集可见的 Dropdown 下拉菜单
 * Ant Design Dropdown 通过 portal 渲染到 document.body
 * DOM 结构: .ant-dropdown > .ant-dropdown-menu > .ant-dropdown-menu-item
 */
function collectVisibleDropdowns(results: Element[]): void {
  const dropdowns = document.querySelectorAll('.ant-dropdown');
  dropdowns.forEach((el) => {
    if (isVisible(el) && !results.includes(el)) {
      results.push(el);
    }
  });
}

/**
 * 收集可见的 Select/Cascader/DatePicker 下拉面板
 * 这些组件的下拉面板也通过 portal 渲染到 document.body
 * DOM 结构: .ant-select-dropdown > .ant-select-item
 */
function collectVisibleSelectDropdowns(results: Element[]): void {
  const selectors = [
    '.ant-select-dropdown',
    '.ant-cascader-dropdown',
    '.ant-picker-dropdown',
    '.ant-tree-select-dropdown',
  ];
  for (const selector of selectors) {
    try {
      const elements = document.querySelectorAll(selector);
      elements.forEach((el) => {
        // 下拉面板可见性：检查是否有 hidden 类或 display:none
        if (!el.classList.contains(`${selector.replace('.', '')}-hidden`) && isVisible(el) && !results.includes(el)) {
          results.push(el);
        }
      });
    } catch {
      // 选择器不兼容，忽略
    }
  }
}

/**
 * 判断元素是否真正可见
 * 综合检查 display、visibility、opacity 以及实际尺寸
 */
function isVisible(element: Element): boolean {
  if (!(element instanceof HTMLElement)) return false;

  const style = window.getComputedStyle(element);
  if (style.display === 'none') return false;
  if (style.visibility === 'hidden') return false;
  if (style.opacity === '0') return false;

  // 检查 width/height 是否为 0（Drawer 关闭时 content-wrapper 可能 width:0）
  // 但对于根容器（.ant-drawer），即使内部 content-wrapper 正在动画，
  // 根容器本身仍然有尺寸，所以这里对根容器不检查尺寸
  return true;
}

/**
 * 判断元素是否在排除范围内
 */
export function isExcluded(element: Element): boolean {
  let current: Element | null = element;
  while (current) {
    if (current.hasAttribute(AI_EXCLUDE_ATTR)) {
      return true;
    }
    current = current.parentElement;
  }
  return false;
}

/**
 * 判断元素是否在AI可见范围内（主内容区或弹框/抽屉内）
 */
export function isInScope(element: Element, scope: ScopeResult): boolean {
  const roots: Element[] = [];
  if (scope.mainRoot) roots.push(scope.mainRoot);
  roots.push(...scope.modalRoots);

  for (const root of roots) {
    if (root.contains(element)) return true;
  }
  return false;
}
