import {  post, del } from '@gwsu/core';
import type { ConfigVO} from '@gwsu/core';
import { ConfigValueType, ConfigType } from '@gwsu/core';
import type { RemoteControlConfig } from '../AssistantConfigTab/types';

const BASE = '/security/config';

/** 配置信息（兼容旧引用） */
export type ConfigInfo = ConfigVO;

export interface ConfigQuery {
  configKey?: string;
  configName?: string;
  valueType?: ConfigValueType;
  configType?: ConfigType;
  modulePrefix?: string;
  pageNum?: number;
  pageSize?: number;
}

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

/** 保存远程操作配置 */
export async function saveRemoteControlConfig(config: RemoteControlConfig) {
  const res = await post<boolean>('/security/connect/config/remote-control', config);
  return res.data;
}
