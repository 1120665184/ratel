package org.quyq.gwsu.common.ai.agui.domain;


import io.agentscope.core.agui.model.RunAgentInput;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description
 */
public record AIRunnerInstanceWrapper(
        RunAgentInput input ,
        SseEmitter emitter
) {
}
