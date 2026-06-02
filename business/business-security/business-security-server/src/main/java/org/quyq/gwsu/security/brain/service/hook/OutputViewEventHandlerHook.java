package org.quyq.gwsu.security.brain.service.hook;


import cn.hutool.json.JSONUtil;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.encoder.AguiEventEncoder;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.hook.ActingChunkEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.RuntimeContextAware;
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
 * 拦截 OutputViewAgent 的流式输出，通过花括号深度计数提取完整 JSON Patch 行
 * <p>
 * LLM 流式输出是逐字符的，一个完整的 JSONL Patch 行会被拆成多个 chunk。
 * 本 Hook 通过花括号深度计数器判断一个完整 JSON 的边界：
 * - 遇到 { 且不在字符串内 → depth++，depth 从 0 变 1 时标记 JSON 开始
 * - 遇到 } 且不在字符串内 → depth--，depth 归 0 时标记 JSON 结束
 * - 字符串内的 { } 不计入深度（通过 " 和 \" 状态跟踪）
 * - 遇到换行符时如果仍在字符串内，说明引号缺失导致字符串未闭合，重置所有状态
 * <p>
 * 输出结束时发送 AGENT_OUTPUT_END 事件，前端据此隐藏"生成中"状态。
 */
@Slf4j
@RequiredArgsConstructor
public class OutputViewEventHandlerHook implements Hook, RuntimeContextAware {

    private final ObjectMapper objectMapper;

    private final AguiEventEncoder encoder = new AguiEventEncoder();

    /**
     * JSON 累积缓冲区：从 { 开始累积字符，到 } 归零时输出
     */
    private final StringBuilder jsonBuffer = new StringBuilder();

    /**
     * 花括号深度计数器：0 表示空闲，> 0 表示正在收集 JSON
     */
    private int braceDepth = 0;

    /**
     * 是否在双引号字符串内
     */
    private boolean inString = false;

    /**
     * 下一个字符是否被转义（遇到 \ 时为 true，处理完一个字符后重置）
     */
    private boolean escape = false;

    private RuntimeContext context;

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof ActingChunkEvent e && !CollectionUtils.isEmpty(e.getChunk().getOutput())) {
            List<ContentBlock> output = e.getChunk().getOutput();
            for (ContentBlock block : output) {

                if (!(block instanceof TextBlock t) || !JSONUtil.isTypeJSON(t.getText())) {
                    continue;
                }

                Event toolEvent = objectMapper.readValue(t.getText(), Event.class);
                Msg message = toolEvent.getMessage();
                //处理输出视图智能体输出的内容
                if (EventType.REASONING == toolEvent.getType() &&
                        MsgRole.ASSISTANT == message.getRole() && OutputViewAgent.AGENT_NAME.equals(message.getName())) {
                    AIRunnerInstanceWrapper sseEmitter = AguiController.getCurrEmitter(context.getSessionId());
                    if (Objects.isNull(sseEmitter)) {
                        log.warn("SSE emitter is null");
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

    }


    /**
     * 处理流式 chunk：逐字符处理，通过花括号深度计数提取完整 JSON
     */
    private void handleChunk(TextBlock textBlock, AIRunnerInstanceWrapper sseEmitter) {
        String text = textBlock.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 如果当前在字符串内
            if (inString) {
                // 处理转义：\" \\ 等转义序列跳过下一个字符
                if (escape) {
                    escape = false;
                    jsonBuffer.append(c);
                    continue;
                }
                // 遇到反斜杠，标记下一个字符为转义
                if (c == '\\') {
                    escape = true;
                    jsonBuffer.append(c);
                    continue;
                }
                // 遇到未转义的 "，字符串结束
                if (c == '"') {
                    inString = false;
                    jsonBuffer.append(c);
                    continue;
                }
                // 字符串内遇到换行符 → 引号缺失导致字符串未闭合，重置状态
                if (c == '\n') {
                    log.warn("Unclosed string detected, resetting parser. Buffer: {}", jsonBuffer);
                    resetParser();
                    continue;
                }
                // 字符串内的其他字符（包括 { } ）正常累积，不影响计数器
                jsonBuffer.append(c);
                continue;
            }

            // 不在字符串内
            if (braceDepth == 0) {
                // 空闲状态：等待 { 开始新 JSON
                if (c == '{') {
                    braceDepth = 1;
                    jsonBuffer.append(c);
                }
                // 忽略非 { 的字符（换行符、空格、markdown 标记等）
                continue;
            }

            // 正在收集 JSON（braceDepth > 0）
            if (c == '{') {
                braceDepth++;
                jsonBuffer.append(c);
            } else if (c == '}') {
                braceDepth--;
                jsonBuffer.append(c);
                if (braceDepth == 0) {
                    // 一个完整 JSON 收集完毕
                    String json = jsonBuffer.toString();
                    jsonBuffer.setLength(0);
                    trySendPatch(json, sseEmitter);
                }
            } else if (c == '"') {
                inString = true;
                jsonBuffer.append(c);
            } else if (c == '\n') {
                // 不在字符串内、正在收集 JSON 时遇到换行符
                // 正常情况：LLM 在两个 patch 之间输出换行符，不应打断当前 JSON
                // 异常情况：JSON 内部出现了裸换行（违反 JSON 规范），但花括号仍可能匹配
                // 安全策略：保留换行符，让后续 JSON 校验来决定是否丢弃
                jsonBuffer.append(c);
            } else {
                jsonBuffer.append(c);
            }
        }
    }

    /**
     * 处理输出结束
     * 如果缓冲区中还有未完成的内容（braceDepth > 0），说明 JSON 不完整，丢弃
     * 发送 AGENT_OUTPUT_END 事件
     */
    private void handleEnd(AIRunnerInstanceWrapper sseEmitter) {
        // 如果还有未完成的 JSON，丢弃
        if (braceDepth > 0) {
            log.warn("Incomplete JSON at end of stream, discarding. BraceDepth: {}, Buffer: {}", braceDepth, jsonBuffer);
            resetParser();
        }

        // 发送结束事件
        RunAgentInput input = sseEmitter.input();
        AguiEvent.Custom endEvent = new AguiEvent.Custom(input.getThreadId(), input.getRunId(),
                "AGENT_OUTPUT_END", TextBlock.builder().text("done").build());
        sendEvent(sseEmitter.emitter(), endEvent);
    }

    /**
     * 尝试发送一个完整的 JSON Patch 行
     * 校验 JSON 格式，合法则发送，不合法则丢弃（后续可用 LLM 矫正）
     */
    private void trySendPatch(String json, AIRunnerInstanceWrapper sseEmitter) {
        // 校验 JSON 格式
        if (!JSONUtil.isTypeJSON(json)) {
            log.warn("Malformed JSON patch, skipping: {}", json);
            // TODO: 后续可使用 LLM 矫正该 JSON
            return;
        }

        log.info("输出面板内容：{}", json);
        RunAgentInput input = sseEmitter.input();
        AguiEvent.Custom customAguiEvent = new AguiEvent.Custom(input.getThreadId(), input.getRunId(),
                "AGENT_OUTPUT", TextBlock.builder().text(json).build());
        sendEvent(sseEmitter.emitter(), customAguiEvent);
    }

    /**
     * 重置解析器状态
     * 在检测到不可恢复的错误时调用（如字符串内换行符、流结束时未完成的 JSON）
     */
    private void resetParser() {
        jsonBuffer.setLength(0);
        braceDepth = 0;
        inString = false;
        escape = false;
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
