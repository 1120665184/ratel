/**
 * 审批阶段
 */
export type ApprovalStage = 'POST_REASONING' | 'POST_ACTING';

/**
 * 审批结果
 */
export type ApprovalResultType = 'APPROVED' | 'REJECTED';

/**
 * CUSTOM 事件 HUMAN_APPROVAL 的 value 结构
 * 对应后端 HumanApprovalInfo record
 */
export interface HumanApprovalPayload {
  /** 审批阶段 */
  stage: ApprovalStage;
  /** 推理后暂停需要审批的信息 */
  reasoningStageInfo: ReasoningStageInfo[] | null;
  /** 行动后暂停需要审批的信息 */
  actingStageInfo: ActingStageInfo | null;
}

/**
 * POST_REASONING 阶段审批信息
 */
export interface ReasoningStageInfo {
  /** 提示文案 */
  tip: string;
  /** 待审批的工具调用信息 */
  toolInfo: {
    type: 'tool_use';
    id: string;
    name: string;
    input: Record<string, unknown>;
    content: string;
  };
}

/**
 * POST_ACTING 阶段审批信息
 */
export interface ActingStageInfo {
  /** 提示文案 */
  tip: string;
  /** 工具执行结果信息 */
  resultInfo: {
    type: 'tool_result';
    id: string;
    name: string;
    output: { type: string; text: string }[];
  };
}
