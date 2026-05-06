package org.quyq.gwsu.common.ai.agui.web;

/**
 * 前端工具执行结果回调请求
 * @param toolCallId 工具调用唯一标识
 * @param success 是否执行成功
 * @param result 执行结果描述
 */
public record WebToolCallbackRequest(
        String toolCallId,
        boolean success,
        String result
) {
}
