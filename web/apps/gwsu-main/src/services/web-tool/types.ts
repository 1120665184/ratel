/**
 * Web工具类型
 */
export type WebToolType = 'AUTO' | 'INTERACTIVE';

/**
 * CUSTOM 事件 TOOL_EXECUTE 的 value 结构
 */
export interface WebToolExecutePayload {
  /** 工具调用唯一标识 */
  toolCallId: string;
  /** 工具名称 */
  toolName: string;
  /** 工具参数 */
  params: Record<string, unknown>;
}

/**
 * 工具执行结果
 */
export interface WebToolResult {
  /** 是否执行成功 */
  success: boolean;
  /** 执行结果描述 */
  result: string;
}

/**
 * 工具执行器接口
 * 每个前端Web工具都需要实现此接口
 */
export interface WebToolExecutor {
  /** 执行工具，返回结果 */
  execute(params: Record<string, unknown>): Promise<WebToolResult>;
}

/**
 * 工具回调请求体（发送给后端）
 */
export interface WebToolCallbackRequest {
  /** 工具调用唯一标识 */
  toolCallId: string;
  /** 是否执行成功 */
  success: boolean;
  /** 执行结果描述 */
  result: string;
}

/**
 * 工具确认事件
 * 当 toolType 为 INTERACTIVE 时，通过此事件通知 UI 展示确认对话框
 */
export interface WebToolConfirmEvent {
  /** 工具调用唯一标识 */
  toolCallId: string;
  /** 工具名称 */
  toolName: string;
  /** 工具描述 */
  description: string;
  /** 工具参数 */
  params: Record<string, unknown>;
  /** 用户确认回调 */
  onConfirm: () => void;
  /** 用户取消回调 */
  onCancel: () => void;
}
