package org.quyq.gwsu.security.headless;

import io.agentscope.core.agui.event.AguiEvent;

import java.util.Map;

/**
 * 无头浏览器审批/提问处理器
 * 注册后，Session 遇到 HUMAN_APPROVAL 或 AskUserQuestion 时自动调用
 */
public interface HeadlessApprovalHandler {

    /**
     * 处理 HUMAN_APPROVAL 事件
     * Session 会根据返回结果自动操作界面上的审批按钮
     *
     * @param event 审批事件（CUSTOM 类型），event.value() 包含审批详情
     * @return 审批结果
     */
    ApprovalResult handleApproval(AguiEvent.Custom event);

    /**
     * 处理 AskUserQuestion 事件
     * Session 会根据返回的答案自动操作界面上的选择框并提交
     *
     * @param toolCallId 工具调用 ID
     * @param questions 问题列表（从 TOOL_CALL_ARGS 中累积解析）
     * @return 问题答案，key 为问题文本，value 为用户选择的答案
     */
    Map<String, String> handleAskUserQuestion(String toolCallId, Map<String, Object> questions);

    /**
     * 审批结果
     */
    record ApprovalResult(boolean approved, String rejectReason) {
        public static ApprovalResult approved() {
            return new ApprovalResult(true, null);
        }

        public static ApprovalResult rejected(String reason) {
            return new ApprovalResult(false, reason);
        }
    }
}
