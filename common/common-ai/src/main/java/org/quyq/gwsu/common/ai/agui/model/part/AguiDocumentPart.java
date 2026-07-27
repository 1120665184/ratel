package org.quyq.gwsu.common.ai.agui.model.part;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.quyq.gwsu.common.ai.agui.model.source.AguiContentSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record AguiDocumentPart(String type, AguiContentSource source, Map<String, Object> metadata)
        implements AguiContentPart {

    @JsonCreator
    public AguiDocumentPart(@JsonProperty("type") String type,
                            @JsonProperty("source") AguiContentSource source,
                            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.type = type == null || type.isBlank() ? "document" : type;
        this.source = source;
        this.metadata = metadata != null ? Collections.unmodifiableMap(new HashMap<>(metadata)) : Collections.emptyMap();
    }
}
