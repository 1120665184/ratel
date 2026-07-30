package org.quyq.gwsu.common.ai.agui.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record AguiFunctionCall(String name, String arguments) {

    @JsonCreator
    public AguiFunctionCall(@JsonProperty("name") String name, @JsonProperty("arguments") String arguments) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.arguments = arguments != null ? arguments : "{}";
    }

}
