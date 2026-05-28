import { get, post, put, del } from '@gwsu/core';

const BASE = '/security/dict';
const VALUE_BASE = '/security/dict-value';

/** 字典信息 */
export interface DictInfo {
  id?: string;
  dictKey: string;
  dictName: string;
  dictType: number;
  description?: string;
  modulePrefix?: string;
  valueCount?: number;
  createTime?: string;
  modifyTime?: string;
}

/** 字典值信息 */
export interface DictValueInfo {
  id?: string;
  dictId: string;
  dictValue: string;
  sort: number;
  createTime?: string;
}

/** 字典查询条件 */
export interface DictQuery {
  dictKey?: string;
  dictName?: string;
  dictType?: number;
  modulePrefix?: string;
  pageNum?: number;
  pageSize?: number;
}

/** 字典分页结果 */
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

/** 查询字典值列表 */
export async function getDictValues(dictId: string) {
  const res = await get<DictValueInfo[]>(`${VALUE_BASE}/list/${dictId}`);
  return res.data ?? [];
}

/** 新增或更新字典值 */
export async function saveOrUpdateDictValue(data: Partial<DictValueInfo>) {
  const res = await post<boolean>(VALUE_BASE, data);
  return res.data;
}

/** 批量删除字典值 */
export async function deleteDictValues(ids: string[]) {
  const res = await del<boolean>(VALUE_BASE, ids);
  return res.data;
}

/** 更新字典值排序 */
export async function updateDictValueSort(ids: string[]) {
  const res = await put<boolean>(`${VALUE_BASE}/sort`, ids);
  return res.data;
}
