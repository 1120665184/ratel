package org.quyq.gwsu.common.ai.loop.domain;

import org.quyq.gwsu.common.ai.loop.ApprovalStage;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description 人工审批提示信息
 */
public record ApprovalTips(
        /**
         * 工具名
         */
        String toolName,
        /**
         * 提示信息
         */
        String tip,
        /**
         * 审批阶段
         */
        ApprovalStage stage
) {
}
