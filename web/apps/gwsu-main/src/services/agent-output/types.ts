/**
 * AGENT_OUTPUT 自定义事件 payload
 * 后端 OutputViewEventHandlerHook 发送的完整 JSONL Patch 行
 */
export interface AgentOutputPayload {
  /** JSONL Patch 行文本 */
  text: string;
}

/**
 * AGENT_OUTPUT_END 自定义事件 payload
 * 后端发送，标识本次 AI 输出结束
 */
export interface AgentOutputEndPayload {
  /** 结束标识 */
  text: string;
}
