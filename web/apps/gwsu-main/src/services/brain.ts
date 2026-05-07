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
 * 工具调用信息
 */
export interface BrainToolCall {
  id: string;
  type: 'function';
  function: {
    name: string;
    arguments: string;
  };
}

/**
 * 消息信息（AG-UI Message 格式）
 */
export interface BrainMessage {
  id: string;
  role: 'user' | 'assistant' | 'system' | 'tool';
  content: string | object[] | null;
  toolCalls?: BrainToolCall[];
  toolCallId?: string | null;
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

/**
 * 审批状态信息
 */
export interface ApprovalStatusInfo {
  /** 审批阶段 */
  stage: 'POST_REASONING' | 'POST_ACTING' | null;
  /** 推理后暂停需要审批的信息 */
  reasoningStageInfo: {
    tip: string;
    toolInfo: {
      type: 'tool_use';
      id: string;
      name: string;
      input: Record<string, unknown>;
      content: string;
    };
  }[] | null;
  /** 行动后暂停需要审批的信息 */
  actingStageInfo: {
    tip: string;
    resultInfo: {
      type: 'tool_result';
      id: string;
      name: string;
      output: { type: string; text: string }[];
    };
  } | null;
}

/**
 * 查询会话审批状态
 * 用于页面刷新后恢复审批卡片
 */
export async function getApprovalStatus(threadId: string): Promise<ApprovalStatusInfo> {
  const response = await get<ApprovalStatusInfo>(
    `/security/brain/approval/status/${threadId}`
  );
  return response.data;
}
