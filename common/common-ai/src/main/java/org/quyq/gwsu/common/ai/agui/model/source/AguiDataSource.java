package org.quyq.gwsu.common.ai.agui.model.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AguiDataSource(String type, String value, String mimeType) implements AguiContentSource {

    @JsonCreator
    public AguiDataSource(@JsonProperty("type") String type, @JsonProperty("value") String value,
                          @JsonProperty("mimeType") String mimeType) {
        this.type = type == null || type.isBlank() ? "data" : type;
        this.value = value;
        this.mimeType = mimeType;
    }
}
