package org.quyq.gwsu.headless.domain;


import lombok.Builder;
import lombok.Getter;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
@Getter
@Builder
public class HeadlessCallConfig {

    /**
     * 标识，用于分隔对话，如果有值，对话唯一标识判断为 sign:userId
     */
    private String sign;

    private String userId;

    private String threadId;

}
