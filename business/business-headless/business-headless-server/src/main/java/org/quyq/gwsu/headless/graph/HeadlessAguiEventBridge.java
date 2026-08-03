package org.quyq.gwsu.headless.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeadlessAguiEventBridge {

    public static final String EVENT_JSON_METADATA_KEY = "headless.agui.event.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ChatResponse toChatResponse(AguiEvent event) {
        try {
            AssistantMessage message = AssistantMessage.builder()
                    .properties(Map.of(EVENT_JSON_METADATA_KEY, OBJECT_MAPPER.writeValueAsString(event)))
                    .content("")
                    .build();
            return new ChatResponse(List.of(new Generation(message)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AGUI 事件桥接序列化失败", e);
        }
    }

    public static AguiEvent fromChatResponse(ChatResponse response) {
        if (response == null) {
            return null;
        }

        Object eventJson = response.getResult().getOutput().getMetadata().get(EVENT_JSON_METADATA_KEY);
        if (eventJson == null) {
            return null;
        }
        String json = String.valueOf(eventJson);
        if (!StringUtils.hasText(json)) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, AguiEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AGUI 事件桥接反序列化失败", e);
        }
    }

    public static AguiEvent.Custom statusEvent(String threadId, String runId, HeadlessAgentStatus status) {
        return new AguiEvent.Custom(threadId, runId, "status", Map.of("status", status.name()));
    }

    public static AguiEvent.Raw rawEvent(String threadId, String runId, String message) {
        return new AguiEvent.Raw(threadId, runId, Map.of("source", "headless", "message", message));
    }

    public static AguiEvent.Raw rawEvent(String threadId, String runId, String code, String message) {
        return new AguiEvent.Raw(threadId, runId, Map.of("source", "headless", "code", code, "message", message));
    }
}
