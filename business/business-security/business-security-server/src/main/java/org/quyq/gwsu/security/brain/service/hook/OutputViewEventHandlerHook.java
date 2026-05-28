package org.quyq.gwsu.security.brain.service.hook;


import cn.hutool.json.JSONUtil;
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
import org.quyq.gwsu.common.ai.constants.AIConstants;
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
 * <p>
 * LLM 流式输出是逐字符的，一个完整的 JSONL Patch 行会被拆成多个 chunk。
 * 本 Hook 维护行缓冲区，只有当收到完整的行（以 \n 结尾）时才发送给前端，
 * 确保 createSpecStreamCompiler 能正确解析每个 patch 操作。
 * <p>
 * 输出结束时发送 AGENT_OUTPUT_END 事件，前端据此隐藏"生成中"状态。
 */
@Slf4j
@RequiredArgsConstructor
public class OutputViewEventHandlerHook implements Hook {

    private final ObjectMapper objectMapper;

    private final AguiEventEncoder encoder = new AguiEventEncoder();

    /**
     * 行缓冲区：累积 LLM 输出直到形成完整行
     */
    private final StringBuilder lineBuffer = new StringBuilder();

    private String threadId;

    /**
     * 上一行发送的 patch，用于去重（防止 LLM 重复输出相同内容）
     */
    private String lastSentLine = null;

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return Mono.deferContextual(ctx ->{
            if(!ctx.isEmpty()){
                threadId = ctx.get(AIConstants.Param.THREAD_ID);
            }

            if (event instanceof ActingChunkEvent e && !CollectionUtils.isEmpty(e.getChunk().getOutput())) {
                List<ContentBlock> output = e.getChunk().getOutput();
                log.info("Acting chunk event: {}", JSONUtil.toJsonStr(output));
                for (ContentBlock block : output) {

                    if (!(block instanceof TextBlock t) || !JSONUtil.isTypeJSON(t.getText())) {
                        continue;
                    }

                    Event toolEvent = objectMapper.readValue(t.getText(), Event.class);
                    Msg message = toolEvent.getMessage();
                    //处理输出视图智能体输出的内容
                    if (EventType.REASONING == toolEvent.getType() &&
                            MsgRole.ASSISTANT == message.getRole() && OutputViewAgent.AGENT_NAME.equals(message.getName())) {
                        AIRunnerInstanceWrapper sseEmitter = AguiController.getCurrEmitter(threadId);
                        if (Objects.isNull(sseEmitter)) {
                            log.info("SSE emitter is null");
                            return Mono.just(event);
                        }

                        message.getContent()
                                .stream()
                                .filter(content -> content instanceof TextBlock)
                                .map(content -> (TextBlock) content)
                                .forEach(text -> {
                                    if (toolEvent.isLast()) {
                                        handleEnd(sseEmitter);
                                    } else {
                                        handleChunk(text, sseEmitter);
                                    }
                                });
                    }


                }


            }

            return Mono.just(event);
        });

    }


    /**
     * 处理流式 chunk
     * 累积到行缓冲区，按 JSON 边界拆分发送完整的 JSONL Patch 行
     */
    private void handleChunk(TextBlock textBlock, AIRunnerInstanceWrapper sseEmitter) {
        log.info("chunk:{}",objectMapper.writeValueAsString(textBlock));
        String text = textBlock.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        lineBuffer.append(text);

        // 按 JSON 边界拆分并发送完整的 patch 行
        flushBuffer(sseEmitter, false);
    }

    /**
     * 处理输出结束
     * 刷新缓冲区剩余内容，发送 AGENT_OUTPUT_END 事件
     */
    private void handleEnd(AIRunnerInstanceWrapper sseEmitter) {

        // 发送所有剩余内容
        flushBuffer(sseEmitter, true);

        // 发送结束事件
        RunAgentInput input = sseEmitter.input();
        AguiEvent.Custom endEvent = new AguiEvent.Custom(input.getThreadId(), input.getRunId(),
                "AGENT_OUTPUT_END", TextBlock.builder().text("done").build());
        sendEvent(sseEmitter.emitter(), endEvent);
    }

    /**
     * 刷新缓冲区：按 JSON 花括号边界拆分，逐个发送完整的 JSON Patch
     * <p>
     * 不依赖 \n 作为分隔符，而是通过花括号深度计数识别完整的 JSON 对象边界。
     * 这样可以同时处理：
     * 1. 正常的 \n 分隔 JSONL
     * 2. 粘连的 }}{ JSON（无换行分隔）
     * 3. JSON 字符串值中包含换行符的情况
     * <p>
     * 同时跳过 JSON 字符串内部的花括号（避免误判），处理转义字符。
     *
     * @param isEnd 是否为输出结束（true 时将不完整的尾部也作为容错发送，false 时保留到缓冲区）
     */
    private void flushBuffer(AIRunnerInstanceWrapper sseEmitter, boolean isEnd) {
        String buffer = lineBuffer.toString();
        lineBuffer.setLength(0);

        int len = buffer.length();
        if (len == 0) {
            return;
        }
        int jsonStart = -1;
        int braceDepth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < len; i++) {
            char c = buffer.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\' && inString) {
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (c == '{') {
                if (braceDepth == 0) {
                    jsonStart = i;
                }
                braceDepth++;
            } else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0 && jsonStart >= 0) {
                    String json = buffer.substring(jsonStart, i + 1).trim();
                    if (!json.isEmpty() && !json.startsWith("```")) {
                        sendAgentOutput(json, sseEmitter);
                    }
                    jsonStart = -1;
                }
            }
        }

        // 处理剩余内容
        if (braceDepth > 0 && jsonStart >= 0) {
            // 不完整的 JSON
            if (isEnd) {
                // 结束模式：容错发送
                String remaining = buffer.substring(jsonStart).trim();
                if (!remaining.isEmpty() && !remaining.startsWith("```")) {
                    sendAgentOutput(remaining, sseEmitter);
                }
            } else {
                // 非结束模式：保留到缓冲区等待后续 chunk
                lineBuffer.append(buffer.substring(jsonStart));
            }
        }
        // braceDepth == 0 且 jsonStart < 0：所有 JSON 已闭合，无剩余内容需保留
    }

    /**
     * 发送单行完整的 JSONL Patch
     * 跳过与上一行相同的 patch（防止 LLM 重复输出）
     */
    private void sendAgentOutput(String line, AIRunnerInstanceWrapper sseEmitter) {
        // 去重：跳过与上一行完全相同的 patch
        if (line.equals(lastSentLine)) {
            log.debug("Skipping duplicate patch: {}", line);
            return;
        }
        lastSentLine = line;

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
