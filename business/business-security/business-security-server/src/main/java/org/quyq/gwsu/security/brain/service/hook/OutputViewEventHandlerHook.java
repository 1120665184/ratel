package org.quyq.gwsu.security.brain.service.hook;


import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agui.encoder.AguiEventEncoder;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.hook.ActingChunkEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.AguiController;
import org.quyq.gwsu.common.ai.agui.domain.AIRunnerInstanceWrapper;
import org.quyq.gwsu.security.brain.service.agent.OutputViewAgent;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 输出视图事件处理 Hook
 * 拦截 OutputViewAgent 的流式输出，按行缓冲后发送 AGENT_OUTPUT 事件
 *
 * LLM 流式输出是逐字符的，一个完整的 JSONL Patch 行会被拆成多个 chunk。
 * 本 Hook 维护行缓冲区，只有当收到完整的行（以 \n 结尾）时才发送给前端，
 * 确保 createSpecStreamCompiler 能正确解析每个 patch 操作。
 *
 * 输出结束时发送 AGENT_OUTPUT_END 事件，前端据此隐藏"生成中"状态。
 */
@Slf4j
@RequiredArgsConstructor
public class OutputViewEventHandlerHook implements Hook {

    private final ObjectMapper objectMapper;

    private final AguiEventEncoder encoder = new AguiEventEncoder();

    /** 行缓冲区：累积 LLM 输出直到形成完整行 */
    private final StringBuilder lineBuffer = new StringBuilder();

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {

        if(event instanceof ActingChunkEvent e && !CollectionUtils.isEmpty(e.getChunk().getOutput())){
            List<ContentBlock> output = e.getChunk().getOutput();

            for(ContentBlock block : output){

                if(!(block instanceof TextBlock t) || !JSONUtil.isTypeJSON(t.getText())){
                    continue;
                }

                Event toolEvent = objectMapper.readValue(t.getText(), Event.class);
                Msg message = toolEvent.getMessage();
                //处理输出视图智能体输出的内容
                if(EventType.REASONING == toolEvent.getType() &&
                        MsgRole.ASSISTANT == message.getRole() && OutputViewAgent.AGENT_NAME.equals(message.getName())){
                    AIRunnerInstanceWrapper sseEmitter = AguiController.getCurrEmitter();
                    if (Objects.isNull(sseEmitter)) {
                        return Mono.just(event);
                    }

                    message.getContent()
                            .stream()
                            .filter(content -> content instanceof TextBlock)
                            .map(content -> (TextBlock) content)
                            .forEach(text ->{
                                if(toolEvent.isLast()){
                                    handleEnd(text , sseEmitter);
                                }else {
                                    handleChunk(text , sseEmitter);
                                }
                            });
                }


            }


        }

        return Mono.just(event);
    }


    /**
     * 处理流式 chunk
     * 累积到行缓冲区，遇到换行符时发送完整的 JSONL Patch 行
     */
    private void handleChunk(TextBlock textBlock ,AIRunnerInstanceWrapper sseEmitter){
        String text = textBlock.getText();
        if(text == null || text.isEmpty()){
            return;
        }

        lineBuffer.append(text);

        // 按换行符拆分，发送完整的行
        String buffer = lineBuffer.toString();
        int newlineIdx;
        while((newlineIdx = buffer.indexOf('\n')) >= 0){
            String line = buffer.substring(0, newlineIdx).trim();
            buffer = buffer.substring(newlineIdx + 1);

            // 跳过空行和 markdown 代码块标记
            if(!line.isEmpty() && !line.startsWith("```")){
                sendAgentOutput(line, sseEmitter);
            }
        }

        // 未消耗的部分保留在缓冲区
        lineBuffer.setLength(0);
        lineBuffer.append(buffer);
    }

    /**
     * 处理输出结束
     * 刷新缓冲区剩余内容，发送 AGENT_OUTPUT_END 事件
     */
    private void handleEnd(TextBlock textBlock ,AIRunnerInstanceWrapper sseEmitter){
        // 将最后一块内容追加到缓冲区
        String text = textBlock.getText();
        if(text != null && !text.isEmpty()){
            lineBuffer.append(text);
        }

        // 刷新缓冲区剩余内容
        String remaining = lineBuffer.toString().trim();
        lineBuffer.setLength(0);

        if(!remaining.isEmpty() && !remaining.startsWith("```")){
            sendAgentOutput(remaining, sseEmitter);
        }

        // 发送结束事件
        RunAgentInput input = sseEmitter.input();
        AguiEvent.Custom endEvent = new AguiEvent.Custom(input.getThreadId(), input.getRunId(),
                "AGENT_OUTPUT_END", TextBlock.builder().text("done").build());
        sendEvent(sseEmitter.emitter(), endEvent);
    }

    /**
     * 发送单行完整的 JSONL Patch
     */
    private void sendAgentOutput(String line, AIRunnerInstanceWrapper sseEmitter){
        RunAgentInput input = sseEmitter.input();
        AguiEvent.Custom customAguiEvent = new AguiEvent.Custom(input.getThreadId(), input.getRunId(),
                "AGENT_OUTPUT", TextBlock.builder().text(line).build());
        sendEvent(sseEmitter.emitter(), customAguiEvent);
    }


    private void sendEvent(SseEmitter emitter, AguiEvent event) {
        try {
            String jsonData = encoder.encodeToJson(event);
            emitter.send(SseEmitter.event().data(jsonData, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.debug("Failed to send SSE event: {}", e.getMessage());
        }
    }

}
