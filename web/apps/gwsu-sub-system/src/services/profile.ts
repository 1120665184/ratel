import { get, put } from '@gwsu/core';

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

/** 修改密码请求参数 */
export interface ChangePasswordParams {
  /** 旧密码 */
  oldPassword: string;
  /** 新密码 */
  newPassword: string;
}

/** 获取当前用户角色列表 */
export async function getCurrentUserRoles(): Promise<RoleVO[]> {
  const res = await get<RoleVO[]>('/security/role/rolesByCurrUser');
  return res.data ?? [];
}

/** 修改当前用户密码 */
export async function changePassword(params: ChangePasswordParams): Promise<void> {
  await put<void>('/system/manager/password', params);
}
