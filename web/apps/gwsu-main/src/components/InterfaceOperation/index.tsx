import { AiModeOverlay } from '@/components/AiModeOverlay';
import { useMenuStore } from '@gwsu/core';
import { useEffect } from 'react';
import { Outlet, useLocation } from 'umi';
import styles from './index.module.less';

interface InterfaceOperationProps {
  children?: React.ReactNode;
}

/**
 * 界面操作能力组件
 * 仅包含微应用界面展示区
 * 菜单导航已移至顶部导航栏的路由选择器
 */
const InterfaceOperation: React.FC<InterfaceOperationProps> = () => {
  const location = useLocation();
  const { menus, loadMenus, updateCurrentMenuRouteByPath } = useMenuStore();

  // 初始化加载菜单
  useEffect(() => {
    if (menus.length === 0) {
      loadMenus().catch(console.error);
    }
  }, []);

  // 菜单加载完成后或路由变化时，更新当前菜单路由
  useEffect(() => {
    if (menus.length > 0) {
      updateCurrentMenuRouteByPath(location.pathname);
    }
  }, [location.pathname, menus, updateCurrentMenuRouteByPath]);

  return (
    <div className={styles.interfaceOperation} data-ai-scope="interface-operation">
      <main className={styles.operationContent}>
        <div className={styles.microAppContainer}>
          <Outlet />
        </div>
      </main>
      <AiModeOverlay />
    </div>
  );
};

export default InterfaceOperation;
