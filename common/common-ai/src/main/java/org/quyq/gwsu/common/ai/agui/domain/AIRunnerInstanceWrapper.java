package org.quyq.gwsu.common.ai.agui.domain;


import io.agentscope.core.agui.encoder.AguiEventEncoder;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description
 */
@Slf4j
public record AIRunnerInstanceWrapper(
        RunAgentInput input ,
        SseEmitter emitter
) {

    public static final AguiEventEncoder ENCODER = new AguiEventEncoder();

    /**
     * 发送事件
     * @param event
     */
    public  void sendEvent( AguiEvent event) {
        try {
            String jsonData = ENCODER.encodeToJson(event);
            emitter.send(SseEmitter.event().data(jsonData, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.debug("Failed to send SSE event: {}", e.getMessage());
        }
    }

}
