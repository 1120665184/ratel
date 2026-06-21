package org.quyq.gwsu.security.headless.domain;


import org.quyq.gwsu.security.headless.enums.HeadlessAgentStatus;
import org.springframework.ai.chat.messages.Message;

/**
 * @author Quyq
 * @date 2026/6/21
 * @description
 */
public record HeadlessResponse(
        HeadlessAgentStatus status ,
        Message message
) {
}
