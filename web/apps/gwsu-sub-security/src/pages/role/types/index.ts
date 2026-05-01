/** 角色信息 */
export interface RoleInfo {
  id?: string;
  roleCode: string;
  roleName: string;
  description?: string;
  sort?: number;
  roleType: number;
  dataScope: number;
  status: boolean;
  createTime?: string;
}

/** 角色查询条件 */
export interface RoleQuery {
  roleName?: string;
  roleType?: number;
  dataScope?: number;
  status?: boolean;
  pageNum?: number;
  pageSize?: number;
}

/** 时效分组 */
export interface ValidGroup {
  roleMenuId: string;
  menuId: string;
  validType: number;
  validStart?: string;
  validEnd?: string;
  cycleType?: number;
  cycleValue?: string;
  cycleStartTime?: string;
  cycleEndTime?: string;
  menuCount: number;
  menuIds: string[];
}

/** 菜单树节点（含角色关联状态） */
export interface MenuTreeNode {
  id: string;
  parentId: string;
  menuName: string;
  menuType: number;
  icon?: string;
  position?: number;
  owner?: number;
  disabled: boolean;
  boundRoleMenuId?: string;
  children?: MenuTreeNode[];
}

/** 时效组保存请求 */
export interface ValidGroupSaveRequest {
  roleMenuId?: string;
  roleId: string;
  validType: number;
  validStart?: string;
  validEnd?: string;
  cycleType?: number;
  cycleValue?: string;
  cycleStartTime?: string;
  cycleEndTime?: string;
  menuIds: string[];
}

/** 数据范围选项 */
export const DATA_SCOPE_OPTIONS = [
  { label: '自定义', value: 0 },
  { label: '全部数据', value: 1 },
  { label: '本部门及以下', value: 2 },
  { label: '本部门', value: 3 },
  { label: '仅本人', value: 4 },
];

/** 角色类型选项 */
export const ROLE_TYPE_OPTIONS = [
  { label: '系统角色', value: 1 },
  { label: '业务角色', value: 2 },
];

/** 时效类型选项 */
export const VALID_TYPE_OPTIONS = [
  { label: '永久', value: 1 },
  { label: '绝对时间范围', value: 2 },
  { label: '周期性', value: 3 },
];

/** 周期类型选项 */
export const CYCLE_TYPE_OPTIONS = [
  { label: '按周', value: 1 },
  { label: '按月', value: 2 },
];

/** 星期选项 */
export const WEEK_DAY_OPTIONS = [
  { label: '周一', value: '1' },
  { label: '周二', value: '2' },
  { label: '周三', value: '3' },
  { label: '周四', value: '4' },
  { label: '周五', value: '5' },
  { label: '周六', value: '6' },
  { label: '周日', value: '7' },
];
