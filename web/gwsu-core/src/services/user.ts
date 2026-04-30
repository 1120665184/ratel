/**
 * 用户服务 API
 */

import { get } from '../utils/request';
import type { UserInfo } from '../stores/userStore';

/**
 * 获取当前登录用户详细信息
 */
export async function fetchCurrentUserInfo(): Promise<UserInfo> {
  const response = await get<UserInfo>('/system/manager/current');
  return response.data;
}
