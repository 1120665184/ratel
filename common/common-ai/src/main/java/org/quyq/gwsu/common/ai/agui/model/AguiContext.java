package org.quyq.gwsu.common.ai.agui.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record AguiContext(String description, String value) {

    @JsonCreator
    public AguiContext(@JsonProperty("description") String description, @JsonProperty("value") String value) {
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

}
