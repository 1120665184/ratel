/**
 * 大脑相关 API 服务
 */

import { post, get, del } from '@gwsu/core';

/**
 * 历史会话信息
 */
export interface BrainHistorySession {
  sessionId: string;
  title: string;
  messageCount: number;
  updatedAt: string;
  timeDisplay: string;
}

/**
 * 消息信息（AG-UI Message 格式）
 */
export interface BrainMessage {
  id: string;
  role: 'user' | 'assistant' | 'system' | 'tool';
  content: string | object[];
  toolCalls?: object;
  toolCallId?: string;
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/**
 * 分页查询历史会话列表
 */
export async function getHistorySessions(
  pageNum: number = 1,
  pageSize: number = 20,
): Promise<PageResult<BrainHistorySession>> {
  const response = await post<PageResult<BrainHistorySession>>('/security/brain/history/sessions', {
    pageNum,
    pageSize,
  });
  return response.data;
}

/**
 * 查询会话消息列表
 */
export async function getSessionMessages(sessionId: string): Promise<BrainMessage[]> {
  const response = await get<BrainMessage[]>(`/security/brain/history/sessions/${sessionId}/messages`);
  return response.data;
}

/**
 * 删除会话
 */
export async function deleteSession(sessionId: string): Promise<boolean> {
  const response = await del<boolean>(`/security/brain/history/sessions/${sessionId}`);
  return response.data;
}
