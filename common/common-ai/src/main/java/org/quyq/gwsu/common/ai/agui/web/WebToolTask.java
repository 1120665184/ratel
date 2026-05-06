package org.quyq.gwsu.common.ai.agui.web;

import java.util.Map;

/**
 * Web工具执行任务，存储在Redis中
 *
 * @param toolCallId 工具调用唯一标识
 * @param toolName   工具名称
 * @param params     工具参数
 * @param status     执行状态
 * @param result     执行结果
 */
public record WebToolTask(
        String toolCallId,
        String toolName,
        Map<String, Object> params,
        WebToolStatus status,
        String result
) {

    /**
     * 创建初始任务（PENDING状态）
     */
    public static WebToolTask pending(String toolCallId, String toolName, Map<String, Object> params) {
        return new WebToolTask(toolCallId, toolName, params, WebToolStatus.PENDING, null);
    }

    /**
     * 更新状态和结果
     */
    public WebToolTask withResult(WebToolStatus status, String result) {
        return new WebToolTask(toolCallId, toolName, params, status, result);
    }
}
