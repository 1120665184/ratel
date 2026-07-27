package org.quyq.gwsu.common.ai.agui.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record AguiMessage(String id, String role, String content, List<AguiToolCall> toolCalls, String toolCallId) {

    @JsonCreator
    public AguiMessage(@JsonProperty("id") String id, @JsonProperty("role") String role,
                       @JsonProperty("content") String content, @JsonProperty("toolCalls") List<AguiToolCall> toolCalls,
                       @JsonProperty("toolCallId") String toolCallId) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.content = content;
        this.toolCalls = toolCalls != null ? Collections.unmodifiableList(toolCalls) : Collections.emptyList();
        this.toolCallId = toolCallId;
    }

    public static AguiMessage userMessage(String id, String content) {
        return new AguiMessage(id, "user", content, null, null);
    }

    public static AguiMessage assistantMessage(String id, String content) {
        return new AguiMessage(id, "assistant", content, null, null);
    }

    public static AguiMessage systemMessage(String id, String content) {
        return new AguiMessage(id, "system", content, null, null);
    }

    public static AguiMessage toolMessage(String id, String toolCallId, String content) {
        return new AguiMessage(id, "tool", content, null, toolCallId);
    }

    public boolean isUserMessage() {
        return "user".equals(role);
    }

    public boolean isAssistantMessage() {
        return "assistant".equals(role);
    }

    public boolean isSystemMessage() {
        return "system".equals(role);
    }

    public boolean isToolMessage() {
        return "tool".equals(role);
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
