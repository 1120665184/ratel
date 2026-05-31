/**
 * 字典服务 API
 */

import { post } from '../utils/request';
import type { DictValueMap } from '../types';

/**
 * 批量获取字典数据
 * 一次请求获取多个字典键对应的值列表，避免多次请求
 *
 * @param dictKeys 字典键列表
 * @returns 字典键 -> 字典值列表的映射
 *
 * @example
 * ```ts
 * import { fetchDictValuesBatch } from '@gwsu/core';
 *
 * // 一次获取多个字典
 * const dictMap = await fetchDictValuesBatch(['user_status', 'gender', 'dept_type']);
 * // dictMap = { user_status: [...], gender: [...], dept_type: [...] }
 * ```
 */
export async function fetchDictValuesBatch(dictKeys: string[]): Promise<DictValueMap> {
  if (!dictKeys.length) return {};
  const res = await post<DictValueMap>('/security/dict/dictValue/getBatch', dictKeys);
  return res.data ?? {};
}
