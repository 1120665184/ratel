package org.quyq.gwsu.security.headless;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.Response;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.event.AguiEventType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * SSE 事件解析器
 *
 * 将 SSE 原始 JSON 解析为 AguiEvent 各子类型，
 * 与后端 AguiController 发送事件的序列化格式对称。
 *
 * JS 拦截器消费 ReadableStream 后，每解析到一个完整 JSON，
 * 通过 exposeFunction 传入 Java，由本解析器反序列化为 AguiEvent 子类型。
 */
@Slf4j
public class HeadlessSseEventParser {

    private final Gson gson = new Gson();

    /**
     * 解析单个 SSE 事件 JSON 为 AguiEvent 子类型
     * 根据 JSON 中的 type 字段路由到对应的 AguiEvent record 构造
     */
    public AguiEvent parseEvent(String eventJson) {
        Map<String, Object> raw;
        try {
            raw = gson.fromJson(eventJson, new TypeToken<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("SSE 事件 JSON 解析失败: {}", e.getMessage());
            return null;
        }
        if (raw == null) return null;

        String typeStr = (String) raw.getOrDefault("type", "");
        AguiEventType type;
        try {
            type = AguiEventType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            // 未知类型，返回 Raw
            return new AguiEvent.Raw(
                    str(raw, "threadId"),
                    str(raw, "runId"),
                    raw
            );
        }

        String threadId = str(raw, "threadId");
        String runId = str(raw, "runId");

        return switch (type) {
            case RUN_STARTED -> new AguiEvent.RunStarted(threadId, runId);
            case RUN_FINISHED -> new AguiEvent.RunFinished(threadId, runId);
            case TEXT_MESSAGE_START -> new AguiEvent.TextMessageStart(
                    threadId, runId,
                    str(raw, "messageId"),
                    str(raw, "role"));
            case TEXT_MESSAGE_CONTENT -> new AguiEvent.TextMessageContent(
                    threadId, runId,
                    str(raw, "messageId"),
                    str(raw, "delta"));
            case TEXT_MESSAGE_END -> new AguiEvent.TextMessageEnd(
                    threadId, runId,
                    str(raw, "messageId"));
            case TOOL_CALL_START -> new AguiEvent.ToolCallStart(
                    threadId, runId,
                    str(raw, "toolCallId"),
                    str(raw, "toolCallName"));
            case TOOL_CALL_ARGS -> new AguiEvent.ToolCallArgs(
                    threadId, runId,
                    str(raw, "toolCallId"),
                    str(raw, "delta"));
            case TOOL_CALL_END -> new AguiEvent.ToolCallEnd(
                    threadId, runId,
                    str(raw, "toolCallId"));
            case TOOL_CALL_RESULT -> new AguiEvent.ToolCallResult(
                    threadId, runId,
                    str(raw, "toolCallId"),
                    str(raw, "content"),
                    str(raw, "role"),
                    str(raw, "messageId"));
            case CUSTOM -> new AguiEvent.Custom(
                    threadId, runId,
                    str(raw, "name"),
                    raw.get("value"));
            case STATE_SNAPSHOT -> new AguiEvent.StateSnapshot(
                    threadId, runId,
                    mapVal(raw, "snapshot"));
            case STATE_DELTA, REASONING_START, REASONING_MESSAGE_START,
                 REASONING_MESSAGE_CONTENT, REASONING_MESSAGE_END,
                 REASONING_MESSAGE_CHUNK, REASONING_END, RAW ->
                    new AguiEvent.Raw(threadId, runId, raw);
        };
    }

    /**
     * 判断 HTTP 响应是否为 SSE 流
     */
    public static boolean isSseResponse(Response response) {
        String contentType = response.headers().get("content-type");
        return contentType != null && contentType.contains("text/event-stream");
    }

    private static String str(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        return v != null ? v.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapVal(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        if (v instanceof Map) return (Map<String, Object>) v;
        return Map.of();
    }
}
