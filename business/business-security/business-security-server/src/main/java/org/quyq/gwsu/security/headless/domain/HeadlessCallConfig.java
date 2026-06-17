package org.quyq.gwsu.security.headless.domain;


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

    private String userId;

    private String threadId;

}
