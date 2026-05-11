/**
 * 按钮权限状态管理
 */

import { create, type UseBoundStore, type StoreApi } from 'zustand';
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
 * 创建或获取单例 Store
 * 通过真实 window 对象挂载，确保主应用和子应用共享同一个 Zustand 实例
 */
function createOrGetStore(): UseBoundStore<StoreApi<AuthState>> {
  if (rawWindow[STORE_KEY]) {
    return rawWindow[STORE_KEY] as UseBoundStore<StoreApi<AuthState>>;
  }

  const store = create<AuthState>((set, get) => ({
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

export const useAuthStore = createOrGetStore();
