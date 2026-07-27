package org.quyq.gwsu.common.ai.agui.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record AguiToolCall(String id, String type, AguiFunctionCall function) {

    @JsonCreator
    public AguiToolCall(@JsonProperty("id") String id, @JsonProperty("type") String type,
                        @JsonProperty("function") AguiFunctionCall function) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = type != null ? type : "function";
        this.function = Objects.requireNonNull(function, "function cannot be null");
    }

    public AguiToolCall(String id, AguiFunctionCall function) {
        this(id, "function", function);
    }

}
