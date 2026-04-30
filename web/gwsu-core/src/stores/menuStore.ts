/**
 * 菜单状态管理
 */

import { create } from 'zustand';
import { MenuItem } from '../services/route';

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

export const useMenuStore = create<MenuState>((set, get) => ({
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
  clearMenus: () => set({ menus: [], loading: false, currentMenuRoute: null }),
  setCurrentMenuRoute: (route) => set({ currentMenuRoute: route }),
  updateCurrentMenuRouteByPath: (path) => {
    const { menus } = get();
    const menu = findMenuByPath(menus, path);
    if (menu) {
      set({ currentMenuRoute: menu });
    }
  },
}));
