/**
 * 认证相关 API 服务
 */

import { post } from '@gwsu/core';

/**
 * 退出登录
 */
export async function logout(): Promise<void> {
  await post('/system/auth/logout');
}
