/**
 * 菜单转换工具
 */

import React from 'react';
import {
  DashboardOutlined,
  SettingOutlined,
  SafetyOutlined,
  UserOutlined,
  MenuOutlined,
  AppstoreOutlined,
  HomeOutlined,
  FileOutlined,
  TeamOutlined,
  LockOutlined,
} from '@ant-design/icons';
import { MenuItem } from '../services/route';
import { MenuPosition, MenuRoute } from '../types/menu';

/**
 * 图标映射表
 */
const iconMap: Record<string, React.FC> = {
  dashboard: DashboardOutlined,
  setting: SettingOutlined,
  security: SafetyOutlined,
  user: UserOutlined,
  menu: MenuOutlined,
  appstore: AppstoreOutlined,
  home: HomeOutlined,
  file: FileOutlined,
  team: TeamOutlined,
  lock: LockOutlined,
};

/**
 * 获取图标组件
 */
export function getIconComponent(iconName: string): React.ReactNode {
  if (!iconName) return undefined;
  const IconComponent = iconMap[iconName.toLowerCase()];
  return IconComponent ? React.createElement(IconComponent) : undefined;
}

/**
 * 将后端菜单数据转换为 Ant Design Menu items
 * 目录类型（menuType=1）且有子菜单时渲染为 SubMenu
 * 菜单类型（menuType=2）始终渲染为 MenuItem，不受 children 影响
 */
export function transformToMenuItems(menus: MenuItem[]): MenuRoute[] {
  return menus
    .filter((m) => m.menuType !== 3 && m.visible && m.position === MenuPosition.SIDEBAR) // 过滤按钮类型、隐藏菜单和非侧边栏菜单
    .sort((a, b) => a.sort - b.sort)
    .map((menu) => {
      // 目录类型（menuType=1）且有子菜单时渲染为 SubMenu
      // 菜单类型（menuType=2）始终渲染为 MenuItem，不受 children 影响
      const shouldRenderAsSubMenu = menu.menuType === 1 && !!menu.children?.length;

      if (shouldRenderAsSubMenu) {
        return {
          key: menu.path,
          icon: getIconComponent(menu.icon),
          label: menu.menuName,
          'data-micro-app': menu.microApp,
          children: transformToMenuItems(menu.children || []),
        } as MenuRoute;
      }

      return {
        key: menu.path,
        icon: getIconComponent(menu.icon),
        label: menu.menuName,
        'data-micro-app': menu.microApp,
      } as MenuRoute;
    });
}
