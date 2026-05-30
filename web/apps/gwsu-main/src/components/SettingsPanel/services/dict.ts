import { get, post, del } from '@gwsu/core';

const BASE = '/security/dict';

export interface DictInfo {
  id?: string;
  dictKey: string;
  dictName: string;
  dictType: number;
  description?: string;
  valueCount?: number;
  createOp?: string;
  createTime?: string;
  modifyOp?: string;
  modifyTime?: string;
}

export interface DictValueInfo {
  id?: string;
  dictKey: string;
  dictValue: string;
  dictLabel: string;
  sort: number;
  createOp?: string;
  createTime?: string;
  modifyOp?: string;
  modifyTime?: string;
}

export interface DictQuery {
  dictKey?: string;
  dictName?: string;
  dictType?: number;
  modulePrefix?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface DictPageResult {
  records: DictInfo[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/** 分页查询字典 */
export async function getDictPage(query: DictQuery) {
  const res = await post<DictPageResult>(`${BASE}/page`, query);
  return res.data;
}

/** 新增或更新字典 */
export async function saveOrUpdateDict(data: Partial<DictInfo>) {
  const res = await post<boolean>(BASE, data);
  return res.data;
}

/** 批量删除字典 */
export async function deleteDicts(ids: string[]) {
  const res = await del<boolean>(BASE, ids);
  return res.data;
}

/** 通过字典键获取值列表 */
export async function getDictValues(dictKey: string) {
  const res = await get<DictValueInfo[]>(`${BASE}/dictValue/get/${dictKey}`);
  return res.data ?? [];
}

/** 保存或更新字典值 */
export async function saveOrUpdateDictValue(data: Partial<DictValueInfo>) {
  const res = await post<boolean>(`${BASE}/dictValue/saveOrUpdate`, data);
  return res.data;
}

/** 批量删除字典值 */
export async function deleteDictValues(ids: string[]) {
  const res = await del<boolean>(`${BASE}/dictValue/removes`, ids);
  return res.data;
}

/** 更新字典值排序 */
export async function updateDictValueSort(dictKey: string, ids: string[]) {
  const res = await post<boolean>(`${BASE}/dictValue/sort/${dictKey}`, ids);
  return res.data;
}
