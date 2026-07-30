package org.quyq.gwsu.common.ai.agui.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quyq.gwsu.common.ai.agui.model.content.AguiMessageContent;
import org.quyq.gwsu.common.ai.agui.model.content.AguiPartsContent;
import org.quyq.gwsu.common.ai.agui.model.content.AguiTextContent;
import org.quyq.gwsu.common.ai.agui.model.part.AguiTextPart;
import org.quyq.gwsu.common.ai.agui.model.serde.AguiMessageContentDeserializer;
import org.quyq.gwsu.common.ai.agui.model.serde.AguiMessageContentSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record AguiMessage(
        String id,
        String role,
        @JsonSerialize(using = AguiMessageContentSerializer.class)
        @JsonDeserialize(using = AguiMessageContentDeserializer.class)
        AguiMessageContent content,
        List<AguiToolCall> toolCalls,
        String toolCallId) {

    @JsonCreator
    public AguiMessage(@JsonProperty("id") String id, @JsonProperty("role") String role,
                       @JsonProperty("content")
                       @JsonSerialize(using = AguiMessageContentSerializer.class)
                       @JsonDeserialize(using = AguiMessageContentDeserializer.class)
                       AguiMessageContent content, @JsonProperty("toolCalls") List<AguiToolCall> toolCalls,
                       @JsonProperty("toolCallId") String toolCallId) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.content = content;
        this.toolCalls = toolCalls != null ? Collections.unmodifiableList(toolCalls) : Collections.emptyList();
        this.toolCallId = toolCallId;
    }

    public static AguiMessage userMessage(String id, String content) {
        return new AguiMessage(id, "user", new AguiTextContent(content), null, null);
    }

    public static AguiMessage assistantMessage(String id, String content) {
        return new AguiMessage(id, "assistant", new AguiTextContent(content), null, null);
    }

    public static AguiMessage systemMessage(String id, String content) {
        return new AguiMessage(id, "system", new AguiTextContent(content), null, null);
    }

    public static AguiMessage toolMessage(String id, String toolCallId, String content) {
        return new AguiMessage(id, "tool", new AguiTextContent(content), null, toolCallId);
    }

    public String textContent() {
        if (content == null) {
            return null;
        }
        if (content instanceof AguiTextContent textContent) {
            return textContent.text();
        }
        if (content instanceof AguiPartsContent partsContent) {
            return partsContent.parts().stream()
                    .filter(AguiTextPart.class::isInstance)
                    .map(AguiTextPart.class::cast)
                    .map(AguiTextPart::text)
                    .filter(Objects::nonNull)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse(null);
        }
        return null;
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
