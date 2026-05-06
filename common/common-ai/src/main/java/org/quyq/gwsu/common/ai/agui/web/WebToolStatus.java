package org.quyq.gwsu.common.ai.agui.web;

/**
 * Web工具任务状态枚举
 */
public enum WebToolStatus {
    /** 等待前端执行 */
    PENDING,
    /** 执行成功 */
    SUCCESS,
    /** 执行失败 */
    FAILED,
    /** 执行超时 */
    TIMEOUT
}
