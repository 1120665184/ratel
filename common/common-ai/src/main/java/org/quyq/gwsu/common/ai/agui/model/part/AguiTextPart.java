package org.quyq.gwsu.common.ai.agui.model.part;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AguiTextPart(String type, String text) implements AguiContentPart {

    @JsonCreator
    public AguiTextPart(@JsonProperty("type") String type, @JsonProperty("text") String text) {
        this.type = type == null || type.isBlank() ? "text" : type;
        this.text = text;
    }

    public static AguiTextPart of(String text) {
        return new AguiTextPart("text", text);
    }
}
