import { get, post, put, del } from '@gwsu/core';
import type {
  RoleInfo,
  RoleQuery,
  ValidGroup,
  MenuTreeNode,
  ValidGroupSaveRequest,
} from '../types';

const BASE = '/security/role';

/** 分页查询角色 */
export async function getRolePage(query: RoleQuery) {
  return post(`${BASE}/page`, query);
}

/** 根据ID查询角色 */
export async function getRoleById(id: string): Promise<RoleInfo> {
  const res = await get<RoleInfo>(`${BASE}/${id}`);
  return res.data;
}

/** 查询角色列表 */
export async function getRoleList(status?: number): Promise<RoleInfo[]> {
  const params: Record<string, unknown> = {};
  if (status !== undefined) {
    params.status = status;
  }
  const res = await get<RoleInfo[]>(`${BASE}/list`, params);
  return res.data;
}

/** 新增或更新角色 */
export async function saveOrUpdateRole(data: RoleInfo): Promise<boolean> {
  const res = await post<boolean>(BASE, data);
  return res.data;
}

/** 批量删除角色 */
export async function deleteRoles(ids: string[]): Promise<boolean> {
  const res = await del<boolean>(BASE, ids);
  return res.data;
}

/** 启用/禁用角色 */
export async function updateRoleStatus(
  id: string,
  status: number,
): Promise<boolean> {
  const res = await put<boolean>(`${BASE}/status`, null, {
    params: { id, status },
  });
  return res.data;
}

/** 获取角色时效分组列表 */
export async function getValidGroups(roleId: string): Promise<ValidGroup[]> {
  const res = await get<ValidGroup[]>(`${BASE}/valid-groups/${roleId}`);
  return res.data;
}

/** 获取完整菜单树（含角色关联状态） */
export async function getMenuTree(
  roleId: string,
  owner?: number,
): Promise<MenuTreeNode[]> {
  const params: Record<string, unknown> = { roleId };
  if (owner !== undefined) {
    params.owner = owner;
  }
  const res = await get<MenuTreeNode[]>(`${BASE}/menu-tree`, params);
  return res.data;
}

/** 新增或更新时效组 */
export async function saveOrUpdateValidGroup(
  data: ValidGroupSaveRequest,
): Promise<boolean> {
  const res = await post<boolean>(`${BASE}/valid-group`, data);
  return res.data;
}

/** 删除时效组 */
export async function deleteValidGroup(roleMenuId: string): Promise<boolean> {
  const res = await del<boolean>(`${BASE}/valid-group/${roleMenuId}`);
  return res.data;
}
