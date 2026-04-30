/**
 * 菜单路由类型定义
 */

import type { ItemType, MenuItemType, SubMenuType } from 'antd/es/menu/interface';

/**
 * 菜单位置类型
 */
export enum MenuPosition {
  SIDEBAR = 1,
  HEADER = 2,
}

/**
 * 菜单所属类型
 */
export enum MenuOwner {
  ADMIN = 1,
  APP = 2,
}

/**
 * 菜单路由项（用于 Ant Design Menu）
 * 支持微应用关联的菜单项类型
 */
export type MenuRoute = (MenuItemType | SubMenuType) & {
  /** 关联的微应用名称（使用 data 属性避免 React 警告） */
  'data-micro-app'?: string;
};

/**
 * 菜单项数组类型（兼容 Ant Design Menu items 属性）
 */
export type MenuItems = ItemType<MenuItemType>[];
