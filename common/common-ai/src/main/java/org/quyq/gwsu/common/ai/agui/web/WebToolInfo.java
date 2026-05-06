package org.quyq.gwsu.common.ai.agui.web;

import java.util.Map;

/**
 * Web工具执行信息，通过CUSTOM事件发送给前端
 *
 * @param toolCallId 工具调用唯一标识
 * @param toolName   工具名称
 * @param params     工具参数
 */
public record WebToolInfo(
        String toolCallId,
        String toolName,
        Map<String, Object> params
) {
}
