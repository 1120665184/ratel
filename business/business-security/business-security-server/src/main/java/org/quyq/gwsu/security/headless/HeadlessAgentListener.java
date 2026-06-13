package org.quyq.gwsu.security.headless;

import io.agentscope.core.agui.event.AguiEvent;

import java.util.Map;

/**
 * 无头浏览器智能体事件监听器
 * 所有回调在 SSE 事件到达时实时触发，无需等待流结束
 */
public interface HeadlessAgentListener {

    // ==================== 通用回调 ====================

    /**
     * 收到任意 SSE 事件（总入口，先于具体回调执行）
     * 可用于日志记录、事件统计等
     */
    default void onEvent(AguiEvent event) {}

    // ==================== 生命周期事件 ====================

    /** RUN_STARTED：智能体开始运行 */
    default void onRunStarted(AguiEvent.RunStarted event) {}

    /** RUN_FINISHED：智能体运行结束 */
    default void onRunFinished(AguiEvent.RunFinished event) {}

    // ==================== 文本消息事件 ====================

    /** TEXT_MESSAGE_START：文本消息开始 */
    default void onTextMessageStart(AguiEvent.TextMessageStart event) {}

    /**
     * TEXT_MESSAGE_CONTENT：文本消息增量内容
     * @param delta 本次增量文本片段
     */
    default void onTextMessageContent(String delta) {}

    /** TEXT_MESSAGE_END：文本消息结束 */
    default void onTextMessageEnd(AguiEvent.TextMessageEnd event) {}

    // ==================== 工具调用事件 ====================

    /** TOOL_CALL_START：工具调用开始 */
    default void onToolCallStart(AguiEvent.ToolCallStart event) {}

    /** TOOL_CALL_ARGS：工具调用参数增量 */
    default void onToolCallArgs(AguiEvent.ToolCallArgs event) {}

    /** TOOL_CALL_END：工具调用结束 */
    default void onToolCallEnd(AguiEvent.ToolCallEnd event) {}

    /** TOOL_CALL_RESULT：工具调用结果 */
    default void onToolCallResult(AguiEvent.ToolCallResult event) {}

    // ==================== 自定义事件 ====================

    /**
     * HUMAN_APPROVAL：人工审批请求（CUSTOM 类型，name=HUMAN_APPROVAL）
     * 如果注册了 HeadlessApprovalHandler，Session 会自动处理
     * 否则需手动调用 Session.approve() / Session.reject()
     */
    default void onHumanApproval(AguiEvent.Custom event) {}

    /**
     * AskUserQuestion：智能体提问
     * 通过 TOOL_CALL_START 的 toolCallName=AskUserQuestion 识别
     * 如果注册了 HeadlessApprovalHandler，Session 会自动处理
     * 否则需手动调用 Session.answerQuestion()
     *
     * @param toolCallId 工具调用 ID
     * @param questions 问题数据（从 TOOL_CALL_ARGS 中累积解析）
     */
    default void onAskUserQuestion(String toolCallId, Map<String, Object> questions) {}

    /** TOOL_EXECUTE：前端工具执行请求（CUSTOM 类型） */
    default void onToolExecute(AguiEvent.Custom event) {}

    /** AGENT_OUTPUT：AI 输出视图（CUSTOM 类型） */
    default void onAgentOutput(AguiEvent.Custom event) {}

    /** 其他未识别的自定义事件 */
    default void onCustomEvent(AguiEvent.Custom event) {}

    // ==================== 状态事件 ====================

    /** STATE_SNAPSHOT：完整状态快照 */
    default void onStateSnapshot(AguiEvent.StateSnapshot event) {}

    /** STATE_DELTA：增量状态变更 */
    default void onStateDelta(AguiEvent.StateDelta event) {}

    // ==================== 错误处理 ====================

    /** SSE 流发生错误 */
    default void onError(Throwable error) {}
}
