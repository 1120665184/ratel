/**
 * 菜单状态管理
 *
 * 使用 vanilla store 共享状态（无 React 依赖），每个应用用自己的 React 创建 Hook 绑定。
 * 这样避免了 qiankun 微前端中多 React 实例导致 "Invalid hook call" 的问题。
 */

import { createStore } from 'zustand/vanilla';
import type { StoreApi } from 'zustand/vanilla';
import { useStore } from 'zustand';
import { MenuItem } from '../services';

/**
 * 获取真实 window 对象，绕过 qiankun JS 沙箱的 Proxy 代理
 * 确保主应用和子应用共享同一个全局状态
 */
const rawWindow: Window & typeof globalThis & Record<string, unknown> = (0, eval)('window');

/** Store 在真实 window 上的挂载键 */
const STORE_KEY = '__GWSU_MENU_STORE__';

interface MenuState {
  /** 菜单列表 */
  menus: MenuItem[];
  /** 加载状态 */
  loading: boolean;
  /** 当前菜单路由 */
  currentMenuRoute: MenuItem | null;
  /** 设置菜单 */
  setMenus: (menus: MenuItem[]) => void;
  /** 设置加载状态 */
  setLoading: (loading: boolean) => void;
  /** 加载菜单 */
  loadMenus: () => Promise<void>;
  /** 清空菜单 */
  clearMenus: () => void;
  /** 设置当前菜单路由 */
  setCurrentMenuRoute: (route: MenuItem | null) => void;
  /** 根据路径匹配并更新当前菜单路由 */
  updateCurrentMenuRouteByPath: (path: string) => void;
}

/**
 * 根据路径在菜单树中查找匹配的菜单项
 */
function findMenuByPath(menus: MenuItem[], path: string): MenuItem | null {
  for (const menu of menus) {
    // 直接匹配
    if (menu.path === path) {
      return menu;
    }
    // 路径前缀匹配（用于子路由）
    if (path.startsWith(menu.path + '/')) {
      // 在子菜单中查找
      if (menu.children?.length) {
        const found = findMenuByPath(menu.children, path);
        if (found) return found;
      }
      return menu;
    }
    // 在子菜单中递归查找
    if (menu.children?.length) {
      const found = findMenuByPath(menu.children, path);
      if (found) return found;
    }
  }
  return null;
}

/**
 * 根据路径在菜单树中查找所有需要展开的父级目录的 key
 * 返回从顶层到选中菜单所在层级的所有目录 path
 */
export function findOpenKeys(menus: MenuItem[], path: string): string[] {
  const openKeys: string[] = [];

  function walk(items: MenuItem[], parents: string[]): boolean {
    for (const menu of items) {
      if (menu.path === path || path.startsWith(menu.path + '/')) {
        // 找到匹配项，收集路径上所有目录的 key
        if (menu.menuType === 1) {
          parents.push(menu.path);
        }
        // 继续在子菜单中查找更深层级
        if (menu.children?.length) {
          walk(menu.children, parents);
        }
        return true;
      }
      // 在子菜单中递归查找
      if (menu.children?.length) {
        const newParents = [...parents];
        if (menu.menuType === 1) {
          newParents.push(menu.path);
        }
        if (walk(menu.children, newParents)) {
          // 找到了，把路径上的目录 key 收集到 openKeys
          openKeys.push(...newParents);
          return true;
        }
      }
    }
    return false;
  }

  walk(menus, []);
  return openKeys;
}

/**
 * 创建或获取单例 vanilla Store
 * 通过真实 window 对象挂载，确保主应用和子应用共享同一个 Zustand 实例
 * 使用 vanilla store（无 React 依赖），避免多 React 实例冲突
 */
function createOrGetStore(): StoreApi<MenuState> {
  if (rawWindow[STORE_KEY]) {
    return rawWindow[STORE_KEY] as StoreApi<MenuState>;
  }

  const store = createStore<MenuState>((set, get) => ({
    menus: [],
    loading: false,
    currentMenuRoute: null,
    setMenus: (menus) => set({ menus }),
    setLoading: (loading) => set({ loading }),
    loadMenus: async () => {
      set({ loading: true });
      try {
        const { fetchUserRoutes } = await import('../services/route');
        const menus = await fetchUserRoutes();
        set({ menus, loading: false });
      } catch (error) {
        set({ loading: false });
        throw error;
      }
    },
    clearMenus: () => {
      set({ menus: [], loading: false, currentMenuRoute: null });
      // 同步清空按钮权限
      import('./authStore').then(({ useAuthStore }) => {
        useAuthStore.getState().clearAuth();
      });
    },
    setCurrentMenuRoute: (route) => set({ currentMenuRoute: route }),
    updateCurrentMenuRouteByPath: (path) => {
      const { menus } = get();
      const menu = findMenuByPath(menus, path);
      if (menu) {
        set({ currentMenuRoute: menu });
        // 同步更新按钮权限
        import('./authStore').then(({ useAuthStore }) => {
          useAuthStore.getState().updateAuthByMenuRoute(menu);
        });
      }
    },
  }));

  rawWindow[STORE_KEY] = store;
  return store;
}

const vanillaStore = createOrGetStore();

/**
 * React Hook — 菜单状态管理
 *
 * 每个应用导入此 Hook 时，使用各自的 React 实例创建绑定，
 * 但底层共享同一个 vanilla store，确保状态跨应用同步。
 */
function useMenuStore(): MenuState;
function useMenuStore<U>(selector: (state: MenuState) => U): U;
function useMenuStore<U>(selector?: (state: MenuState) => U): MenuState | U {
  return useStore(vanillaStore, selector as (state: MenuState) => U);
}

// 挂载 vanilla store 方法，保持向后兼容（useMenuStore.getState() 等）
useMenuStore.getState = vanillaStore.getState;
useMenuStore.setState = vanillaStore.setState;
useMenuStore.subscribe = vanillaStore.subscribe;
useMenuStore.getInitialState = vanillaStore.getInitialState;

export { useMenuStore };
