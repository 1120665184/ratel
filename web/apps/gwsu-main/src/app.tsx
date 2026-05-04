/**
 * 应用初始化配置
 */

import { useMenuStore, useUserStore } from '@gwsu/core';

/**
 * 路由变化时检查菜单加载
 */
export function onRouteChange({
  location,
}: {
  location: { pathname: string };
}) {
  const isLoggedIn = useUserStore.getState().checkLogin();

  const { menus, loadMenus } = useMenuStore.getState();
  // 已登录但菜单为空时，重新加载菜单
  if (
    isLoggedIn &&
    menus.length === 0 &&
    !location.pathname.includes('/login')
  ) {
    loadMenus().catch(console.error);
  }
}
