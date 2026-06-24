package org.quyq.gwsu.security.headless.enums;


import lombok.Getter;

/**
 * @author Quyq
 * @date 2026/6/21
 * @description
 */
@Getter
public enum HeadlessAgentStatus {
    CONNECTION("连接中"),
    INITING("初始化中"),
    THINKING("思考中"),
    OUTPUTTING("输出中"),
    CALLING("能力调用中"),
    SHOWING("展示中"),
    ERROR("错误"),
    COMPLETE("完成")
    ;

    private final String status;

    HeadlessAgentStatus(String status) {
        this.status = status;
    }

}
