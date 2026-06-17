package org.quyq.gwsu.security.headless.service;


import io.agentscope.core.message.Msg;
import org.quyq.gwsu.security.headless.domain.HeadlessCallConfig;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
public interface IHeadlessService {


    Flux<Message> stream(String query, HeadlessCallConfig config);

}
