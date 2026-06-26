package org.quyq.gwsu.headless.api.vo;


import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;

/**
 * @author Quyq
 * @date 2026/6/21
 * @description
 */
public record HeadlessResponse(
        HeadlessAgentStatus status ,
        AssistantMsg message ,
        String errorMessage
) {

    /**
     * 正常响应（无错误信息）
     */
    public HeadlessResponse(HeadlessAgentStatus status, AssistantMsg message) {
        this(status, message, null);
    }

    /**
     * 错误响应
     */
    public static HeadlessResponse error(String errorMessage) {
        return new HeadlessResponse(HeadlessAgentStatus.ERROR, AssistantMsg.empty(), errorMessage);
    }

    public static HeadlessResponse busy() {
        return new HeadlessResponse(HeadlessAgentStatus.BUSY, AssistantMsg.empty(), "助手正在回答中，请稍后尝试...");
    }


}
