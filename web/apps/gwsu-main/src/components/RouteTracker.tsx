import { useEffect } from 'react';
import { useLocation } from 'umi';
import { useForwardedPropsStore } from '@/stores/forwardedProps';

/**
 * 路由监听组件
 * 监听路由变化并同步到 forwardedProps store，使 CopilotKit 每次请求携带最新的路由信息
 * 必须放在 CopilotKit Provider 内部
 */
export function RouteTracker() {
  const location = useLocation();
  const setCurrentPath = useForwardedPropsStore((s) => s.setCurrentPath);

  useEffect(() => {
    setCurrentPath(location.pathname);
  }, [location.pathname, setCurrentPath]);

  return null;
}
