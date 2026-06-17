package org.quyq.gwsu.security.headless.graph;


import com.alibaba.cloud.ai.agent.agentscope.AgentScopeMessageUtils;
import com.google.gson.Gson;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ThinkingBlock;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.security.headless.HeadlessAgentListener;
import org.quyq.gwsu.security.headless.session.HeadlessPageWrapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
@Slf4j
public class HeadlessMessageHandler implements HeadlessAgentListener {

    private final Sinks.Many<ChatResponse> sink = Sinks.many().multicast().onBackpressureBuffer();

    Gson gson = new Gson();


    @Override
    public void onTextMessageContent(String delta, HeadlessPageWrapper wrapper) {
       // log.info("内容输出：{}", delta);
        sink.tryEmitNext(getContent(delta));

    }


    @Override
    public void onEvent(AguiEvent event, HeadlessPageWrapper wrapper) {
        if (event instanceof AguiEvent.ReasoningMessageContent e) {
            sink.tryEmitNext(getReasoning(e.delta()));
        }
    }


    @Override
    public void onHumanApproval(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {
        sink.tryEmitNext(getContent(gson.toJson(event.value())));
    }

    @Override
    public void onAskUserQuestion(String toolCallId, Map<String, Object> questions, HeadlessPageWrapper wrapper) {
        sink.tryEmitNext(getContent(gson.toJson(questions)));
    }

    @Override
    public void onError(Throwable error, HeadlessPageWrapper wrapper) {
        sink.tryEmitError(error);
    }

    public Flux<ChatResponse> asFlux() {
        return sink.asFlux();
    }


    public void complete() {
        sink.tryEmitComplete();
    }

    private ChatResponse getContent(String delta) {

        AssistantMessage message = AgentScopeMessageUtils.toAssistantMessage(Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent(delta)
                .build());

        return new ChatResponse(List.of(new Generation(message)));
    }

    private ChatResponse getReasoning(String reasoning) {
        AssistantMessage message = AgentScopeMessageUtils.toAssistantMessage(Msg.builder()
                .role(MsgRole.ASSISTANT)
                .content(ThinkingBlock.builder()
                        .thinking(reasoning)
                        .build())
                .build());
        return new ChatResponse(List.of(new Generation(message)));
    }



}
