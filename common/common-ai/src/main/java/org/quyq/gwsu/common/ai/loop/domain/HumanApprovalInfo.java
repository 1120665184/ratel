package org.quyq.gwsu.common.ai.loop.domain;


import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description
 */
public record HumanApprovalInfo(
        ApprovalStage stage,
        /**
         * 推理后暂停需要审批的信息
         */
        List<ReasoningStateInfo>  reasoningStageInfo,
        /**
         * 行动后暂停需要审批的信息
         */
        ActingStageInfo  actingStageInfo
) {

    public record ReasoningStateInfo(
            String tip ,
            ToolUseBlock toolInfo
    ){}


    public record ActingStageInfo(
            String tip ,
            ToolResultBlock resultInfo
    ){ }

}
