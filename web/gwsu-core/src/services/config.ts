/**
 * 配置服务 API
 */

import { post } from '../utils/request';
import type { ConfigMap } from '../types';

/**
 * 批量获取系统配置
 * 一次请求获取多个配置键对应的配置信息，避免多次请求
 *
 * @param configKeys 配置键列表
 * @returns 配置键 -> 配置信息的映射
 *
 * @example
 * ```ts
 * import { fetchConfigsBatch } from '@gwsu/core';
 *
 * // 一次获取多个配置
 * const configMap = await fetchConfigsBatch(['site_name', 'max_upload_size', 'enable_register']);
 * // configMap = { site_name: {...}, max_upload_size: {...}, enable_register: {...} }
 * ```
 */
export async function fetchConfigsBatch(configKeys: string[]): Promise<ConfigMap> {
  if (!configKeys.length) return {};
  const res = await post<ConfigMap>('/security/config/key/get', configKeys);
  return res.data ?? {};
}
