/**
 * 路由服务
 */

import { get } from '../utils/request';
import { MenuOwner, MenuPosition } from '../types/menu';

/**
 * 菜单项数据结构
 */
export interface MenuItem {
  /** 主键ID */
  id: number;
  /** 父菜单ID */
  parentId: number | null;
  /** 菜单名称 */
  menuName: string;
  /** 菜单类型：1-目录 2-菜单 3-按钮 */
  menuType: number;
  /** 排序号 */
  sort: number;
  /** 菜单图标 */
  icon: string;
  /** 路由路径 */
  path: string;
  /** 子应用名称 */
  microApp?: string;
  /** 是否显示 */
  visible: boolean;
  /** 状态：true-正常 false-禁用 */
  status: boolean;
  /** 权限标识 */
  permission?: string;
  /** 菜单位置类型 */
  position?: MenuPosition;
  /** 菜单所属类型 */
  owner?: MenuOwner;
  /** 子菜单列表 */
  children?: MenuItem[];
}

/**
 * 获取当前用户路由菜单
 * 固定获取 ADMIN 类型的菜单
 */
export async function fetchUserRoutes(): Promise<MenuItem[]> {
  const response = await get<MenuItem[]>(`/security/menu/routes/${MenuOwner.ADMIN}`);
  return response.data;
}
