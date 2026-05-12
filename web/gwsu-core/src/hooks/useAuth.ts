/**
 * 按钮权限 Hook
 */

import { useAuthStore } from '../stores';

/**
 * 判断当前路由下是否拥有指定按钮权限
 * @param buttonKey 按钮标识
 * @returns 是否有权限
 */
export function useAuth(buttonKey: string): boolean {
  const buttonAuthMap = useAuthStore((state) => state.buttonAuthMap);
  return buttonAuthMap[buttonKey];
}
