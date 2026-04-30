import { MenuFoldOutlined, MenuUnfoldOutlined, LoadingOutlined } from '@ant-design/icons';
import { Menu, Spin } from 'antd';
import { useThemeContext, useMenuStore, transformToMenuItems, findOpenKeys } from '@gwsu/core';
import { useState, useEffect, useMemo, useCallback } from 'react';
import { history, MicroApp, useLocation } from 'umi';
import styles from './index.module.less';

/**
 * 界面操作能力组件
 * 包含菜单栏和微应用界面展示
 */
const InterfaceOperation: React.FC = () => {
  const location = useLocation();
  const { currentTheme } = useThemeContext();
  const { menus, loading, loadMenus, currentMenuRoute, updateCurrentMenuRouteByPath } = useMenuStore();
  const [collapsed, setCollapsed] = useState(false);
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // 根据当前路径计算需要展开的目录
  const defaultOpenKeys = useMemo(() => findOpenKeys(menus, location.pathname), [menus, location.pathname]);

  // 菜单首次加载或路径变化时，同步 openKeys（仅在 openKeys 为空时自动设置）
  useEffect(() => {
    if (defaultOpenKeys.length > 0) {
      setOpenKeys((prev) => {
        if (prev.length === 0 || !prev.every((k) => defaultOpenKeys.includes(k))) {
          return defaultOpenKeys;
        }
        return prev;
      });
    }
  }, [defaultOpenKeys]);

  // 手动展开/收起目录
  const handleOpenChange = useCallback((keys: string[]) => {
    setOpenKeys(keys);
  }, []);

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

  // 转换菜单数据
  const menuItems = useMemo(() =>
    menus.length > 0 ? transformToMenuItems(menus) : [],
    [menus]
  );

  // 从当前菜单路由中获取微应用名称
  const currentApp = currentMenuRoute?.microApp || 'gwsu-sub-system';

  return (
    <div className={styles.interfaceOperation}>
      {/* 左侧菜单栏 */}
      <aside
        className={styles.operationSider}
        style={{
          width: collapsed ? 64 : 200,
          background: currentTheme.colors.surface,
        }}
      >
        <div className={`${styles.menuWrapper} ${collapsed ? styles.menuWrapperCollapsed : ''}`}>
          {loading ? (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
              <Spin indicator={<LoadingOutlined spin />} />
            </div>
          ) : (
            <Menu
              mode="inline"
              inlineCollapsed={collapsed}
              selectedKeys={[location.pathname]}
              openKeys={collapsed ? [] : openKeys}
              onOpenChange={handleOpenChange}
              items={menuItems}
              onClick={({ key }) => history.push(key)}
              className={styles.menu}
            />
          )}
        </div>
        <div
          className={styles.collapseBtn}
          onClick={() => setCollapsed(!collapsed)}
          style={{ background: currentTheme.colors.surface }}
        >
          {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        </div>
      </aside>

      {/* 界面展示区 */}
      <main
        className={styles.operationContent}
        style={{ background: currentTheme.colors.background }}
      >
        <div className={styles.microAppContainer}>
          <MicroApp name={currentApp} />
        </div>
      </main>
    </div>
  );
};

export default InterfaceOperation;
