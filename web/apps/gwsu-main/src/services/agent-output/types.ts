/**
 * AGENT_OUTPUT 自定义事件 payload
 * 后端 OutputViewEventHandlerHook 发送的 JSONL patch 数据
 */
export interface AgentOutputPayload {
  /** JSONL patch 字符串或完整 spec JSON */
  text: string;
}
