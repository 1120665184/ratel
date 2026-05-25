import { get } from '@gwsu/core';

/** 角色 VO（对应后端 RoleVO） */
export interface RoleVO {
  /** 角色 ID */
  id: string;
  /** 角色名称 */
  roleName: string;
  /** 角色编码 */
  roleCode: string;
  /** 排序 */
  sort?: number;
  /** 描述 */
  description?: string;
  /** 角色类型 */
  roleType?: number;
  /** 数据范围 */
  dataScope?: number;
  /** 状态 */
  status: boolean;
}

/** 获取当前用户角色列表 */
export async function getCurrentUserRoles(): Promise<RoleVO[]> {
  const res = await get<RoleVO[]>('/security/role/rolesByCurrUser');
  return res.data ?? [];
}
