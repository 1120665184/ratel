import { get, post, del } from '@gwsu/core';

const BASE = '/security/config';

/** 配置信息 */
export interface ConfigInfo {
  id?: string;
  configKey: string;
  configName: string;
  configValue: string;
  valueType: number;
  configType: number;
  description?: string;
  modulePrefix?: string;
  createTime?: string;
  modifyTime?: string;
}

/** 配置查询条件 */
export interface ConfigQuery {
  configKey?: string;
  configName?: string;
  valueType?: number;
  configType?: number;
  modulePrefix?: string;
  pageNum?: number;
  pageSize?: number;
}

/** 分页结果 */
export interface ConfigPageResult {
  records: ConfigInfo[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/** 分页查询配置 */
export async function getConfigPage(query: ConfigQuery) {
  const res = await post<ConfigPageResult>(`${BASE}/page`, query);
  return res.data;
}

/** 根据ID查询配置 */
export async function getConfigById(id: string) {
  const res = await get<ConfigInfo>(`${BASE}/${id}`);
  return res.data;
}

/** 根据Key查询配置 */
export async function getConfigByKey(configKey: string) {
  const res = await get<ConfigInfo>(`${BASE}/key/${configKey}`);
  return res.data;
}

/** 新增或更新配置 */
export async function saveOrUpdateConfig(data: Partial<ConfigInfo>) {
  const res = await post<boolean>(BASE, data);
  return res.data;
}

/** 批量删除配置 */
export async function deleteConfigs(ids: string[]) {
  const res = await del<boolean>(BASE, ids);
  return res.data;
}
