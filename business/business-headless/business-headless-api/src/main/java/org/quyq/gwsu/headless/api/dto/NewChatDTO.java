package org.quyq.gwsu.headless.api.dto;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Quyq
 * @date 2026/6/29
 * @description
 */
@Data
@Accessors(chain = true)
public class NewChatDTO {

    private String sign;

    private String userId;

    private String threadId;


}
