import { get, post, put, del } from '@gwsu/core';
import type {
  SysUserVO,
  SysUserDetailVO,
  SysUserQueryDTO,
  SysAccountBindDTO,
  DeptUserCountMap,
} from '@/pages/user/types';

/** 分页查询用户 */
export async function getUserPage(query: SysUserQueryDTO) {
  const res = await post<{ records: SysUserVO[]; total: number; current: number; size: number }>(
    '/system/manager/page',
    query,
  );
  return res.data;
}

/** 获取用户详情 */
export async function getUserDetail(id: string): Promise<SysUserDetailVO> {
  const res = await get<SysUserDetailVO>(`/system/manager/${id}`);
  return res.data;
}

/** 新增或编辑用户 */
export async function saveOrUpdateUser(data: SysUserVO): Promise<string> {
  const res = await post<string>('/system/manager', data);
  return res.data;
}

/** 启禁用用户 */
export async function updateUserStatus(id: string, status: number): Promise<void> {
  await put<void>(`/system/manager/${id}/status?status=${status}`);
}

/** 绑定账号 */
export async function bindAccount(userId: string, data: SysAccountBindDTO): Promise<void> {
  await post<void>(`/system/manager/${userId}/account`, data);
}

/** 解绑账号 */
export async function unbindAccount(userId: string, accountId: string): Promise<void> {
  await del<void>(`/system/manager/${userId}/account/${accountId}`);
}

/** 获取各部门用户数量 */
export async function getDeptUserCount(): Promise<DeptUserCountMap> {
  const res = await get<DeptUserCountMap>('/system/dept/user-count');
  return res.data;
}

/** 批量删除用户 */
export async function batchDeleteUsers(ids: string[]): Promise<void> {
  await del<void>('/system/manager', ids);
}

/** 重置用户密码 */
export async function resetPassword(userId: string, newPassword: string): Promise<void> {
  await put<void>(`/system/manager/${userId}/password`, { newPassword });
}
