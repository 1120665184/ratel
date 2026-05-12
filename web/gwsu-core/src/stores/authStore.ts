/**
 * 按钮权限状态管理
 *
 * 使用 vanilla store 共享状态（无 React 依赖），每个应用用自己的 React 创建 Hook 绑定。
 * 这样避免了 qiankun 微前端中多 React 实例导致 "Invalid hook call" 的问题。
 */

import { createStore } from 'zustand/vanilla';
import type { StoreApi } from 'zustand/vanilla';
import { useStore } from 'zustand';
import { MenuItem } from '../services/route';

/**
 * 获取真实 window 对象，绕过 qiankun JS 沙箱的 Proxy 代理
 * 确保主应用和子应用共享同一个全局状态
 */
const rawWindow: Window & typeof globalThis & Record<string, unknown> = (0, eval)('window');

/** Store 在真实 window 上的挂载键 */
const STORE_KEY = '__GWSU_AUTH_STORE__';

/** 按钮类型菜单 */
const MENU_TYPE_BUTTON = 3;

interface AuthState {
  /** 当前路由的按钮权限映射：buttonKey → true */
  buttonAuthMap: Record<string, boolean>;
  /** 根据当前菜单路由更新权限映射 */
  updateAuthByMenuRoute: (menuRoute: MenuItem | null) => void;
  /** 判断某 buttonKey 是否有权限 */
  hasAuth: (buttonKey: string) => boolean;
  /** 清空权限（退出登录时） */
  clearAuth: () => void;
}

/**
 * 从菜单路由的 children 中提取按钮权限
 * 筛选 menuType === 3 的项，将其 buttonKey 加入映射
 */
function extractButtonAuths(menuRoute: MenuItem | null): Record<string, boolean> {
  if (!menuRoute?.children?.length) {
    return {};
  }

  const authMap: Record<string, boolean> = {};
  for (const child of menuRoute.children) {
    if (child.menuType === MENU_TYPE_BUTTON && child.buttonKey) {
      authMap[child.buttonKey] = true;
    }
  }
  return authMap;
}

/**
 * 创建或获取单例 vanilla Store
 * 通过真实 window 对象挂载，确保主应用和子应用共享同一个 Zustand 实例
 * 使用 vanilla store（无 React 依赖），避免多 React 实例冲突
 */
function createOrGetStore(): StoreApi<AuthState> {
  if (rawWindow[STORE_KEY]) {
    return rawWindow[STORE_KEY] as StoreApi<AuthState>;
  }

  const store = createStore<AuthState>((set, get) => ({
    buttonAuthMap: {},
    updateAuthByMenuRoute: (menuRoute) => {
      const buttonAuthMap = extractButtonAuths(menuRoute);
      set({ buttonAuthMap });
    },
    hasAuth: (buttonKey) => {
      return !!get().buttonAuthMap[buttonKey];
    },
    clearAuth: () => set({ buttonAuthMap: {} }),
  }));

  rawWindow[STORE_KEY] = store;
  return store;
}

const vanillaStore = createOrGetStore();

/**
 * React Hook — 按钮权限状态管理
 *
 * 每个应用导入此 Hook 时，使用各自的 React 实例创建绑定，
 * 但底层共享同一个 vanilla store，确保状态跨应用同步。
 */
function useAuthStore(): AuthState;
function useAuthStore<U>(selector: (state: AuthState) => U): U;
function useAuthStore<U>(selector?: (state: AuthState) => U): AuthState | U {
  return useStore(vanillaStore, selector as (state: AuthState) => U);
}

// 挂载 vanilla store 方法，保持向后兼容（useAuthStore.getState() 等）
useAuthStore.getState = vanillaStore.getState;
useAuthStore.setState = vanillaStore.setState;
useAuthStore.subscribe = vanillaStore.subscribe;
useAuthStore.getInitialState = vanillaStore.getInitialState;

export { useAuthStore };
