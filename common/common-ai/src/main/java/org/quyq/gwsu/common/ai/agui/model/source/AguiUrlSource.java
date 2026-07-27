package org.quyq.gwsu.common.ai.agui.model.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AguiUrlSource(String type, String value, String mimeType) implements AguiContentSource {

    @JsonCreator
    public AguiUrlSource(@JsonProperty("type") String type, @JsonProperty("value") String value,
                         @JsonProperty("mimeType") String mimeType) {
        this.type = type == null || type.isBlank() ? "url" : type;
        this.value = value;
        this.mimeType = mimeType;
    }
}
