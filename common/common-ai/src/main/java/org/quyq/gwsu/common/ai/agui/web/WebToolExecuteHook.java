package org.quyq.gwsu.common.ai.agui.web;


import com.google.gson.Gson;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.encoder.AguiEventEncoder;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.hook.ActingChunkEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.RuntimeContextAware;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.AguiController;
import org.quyq.gwsu.common.ai.agui.domain.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Objects;

/**
 * 监听是否有需要浏览器端执行的工具事件，有的话发送给浏览器
 */
@Slf4j
public class WebToolExecuteHook implements Hook , RuntimeContextAware {


    private final AguiEventEncoder encoder = new AguiEventEncoder();

    private final Gson gson = new Gson();

    private RuntimeContext context;

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof ActingChunkEvent e && !CollectionUtils.isEmpty(e.getChunk().getOutput())) {
            ContentBlock block = e.getChunk().getOutput().getFirst();
            if (block instanceof TextBlock t) {
                //调用view端工具事件处理
                if (t.getText().startsWith(WebToolUtils.WEB_TOOL_IDENTIFICATION)) {
                    Mono<T> tMono = handlerWebHandlerEvent(t, event);
                    if (Objects.nonNull(tMono)) {
                        return tMono;
                    }
                }

            }

        }

        return Mono.just(event);
    }

    /**
     * 处理调用web视图端工具事件
     *
     * @param t
     * @param event
     * @param <T>
     * @return
     */
    private <T extends HookEvent> Mono<T> handlerWebHandlerEvent(TextBlock t, T event) {

        WebToolInfo info = gson.fromJson(
                t.getText().replace(WebToolUtils.WEB_TOOL_IDENTIFICATION, ""),
                WebToolInfo.class
        );
        AIRunnerInstanceWrapper sseEmitter = AguiController.getCurrEmitter(context.getSessionId());
        if (Objects.isNull(sseEmitter)) {
            return Mono.just(event);
        }

        //发送工具执行自定义事件
        AguiEvent.Custom customAguiEvent = new AguiEvent.Custom(context.getSessionId(), sseEmitter.input().getRunId(),
                AIConstants.AguiCustomEvent.TOOL_EXECUTE
                , info);
        sendEvent(sseEmitter.emitter(), customAguiEvent);

        return null;
    }


    private void sendEvent(SseEmitter emitter, AguiEvent event) {
        try {
            String jsonData = encoder.encodeToJson(event);
            emitter.send(SseEmitter.event().data(jsonData, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.debug("Failed to send SSE event: {}", e.getMessage());
        }
    }

    @Override
    public void setRuntimeContext(RuntimeContext context) {
        this.context = context;
    }
}
