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

/** 枚举选项（从后端获取） */
export interface EnumOption {
  label: string;
  value: number;
}

/** 时效类型选项（前端固定，不依赖后端） */
export const VALID_TYPE_OPTIONS = [
  { label: '永久', value: 1 },
  { label: '绝对时间范围', value: 2 },
  { label: '周期性', value: 3 },
];

/** 周期类型选项（前端固定，不依赖后端） */
export const CYCLE_TYPE_OPTIONS = [
  { label: '按周', value: 1 },
  { label: '按月', value: 2 },
];

/** 星期选项（前端固定，不依赖后端） */
export const WEEK_DAY_OPTIONS = [
  { label: '周一', value: '1' },
  { label: '周二', value: '2' },
  { label: '周三', value: '3' },
  { label: '周四', value: '4' },
  { label: '周五', value: '5' },
  { label: '周六', value: '6' },
  { label: '周日', value: '7' },
];
