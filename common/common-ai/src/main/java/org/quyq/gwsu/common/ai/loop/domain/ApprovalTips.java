package org.quyq.gwsu.common.ai.loop.domain;


/**
 * @author Quyq
 * @date 2026/5/6
 * @description
 */
public record ApprovalTips(
        /**
         * 工具名
         */
        String toolName ,
        /**
         * 提示信息
         */
        String tip
) {
}
