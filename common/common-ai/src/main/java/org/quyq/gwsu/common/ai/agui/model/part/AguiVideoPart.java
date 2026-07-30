package org.quyq.gwsu.common.ai.agui.model.part;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quyq.gwsu.common.ai.agui.model.source.AguiContentSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record AguiVideoPart(String type, AguiContentSource source, Map<String, Object> metadata)
        implements AguiContentPart {

    @JsonCreator
    public AguiVideoPart(@JsonProperty("type") String type,
                         @JsonProperty("source") AguiContentSource source,
                         @JsonProperty("metadata") Map<String, Object> metadata) {
        this.type = type == null || type.isBlank() ? "video" : type;
        this.source = source;
        this.metadata = metadata != null ? Collections.unmodifiableMap(new HashMap<>(metadata)) : Collections.emptyMap();
    }
}
