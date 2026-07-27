package org.quyq.gwsu.common.ai.agui.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record AguiTool(String name, String description, Map<String, Object> parameters) {

    @JsonCreator
    public AguiTool(@JsonProperty("name") String name, @JsonProperty("description") String description,
                    @JsonProperty("parameters") Map<String, Object> parameters) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.parameters = parameters != null ? Collections.unmodifiableMap(new HashMap<>(parameters)) : Collections.emptyMap();
    }

}
