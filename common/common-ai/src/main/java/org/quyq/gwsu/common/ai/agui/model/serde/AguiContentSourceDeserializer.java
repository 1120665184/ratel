package org.quyq.gwsu.common.ai.agui.model.serde;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import org.quyq.gwsu.common.ai.agui.model.source.AguiContentSource;
import org.quyq.gwsu.common.ai.agui.model.source.AguiDataSource;
import org.quyq.gwsu.common.ai.agui.model.source.AguiUrlSource;
public class AguiContentSourceDeserializer extends ValueDeserializer<AguiContentSource> {

    @Override
    public AguiContentSource deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.readValueAsTree();
        if (node == null || node.isNull()) {
            return null;
        }

        String type = text(node.get("type"));
        String value = text(node.get("value"));
        String mimeType = text(node.get("mimeType"));

        if ("data".equalsIgnoreCase(type)) {
            return new AguiDataSource(type, value, mimeType);
        }
        return new AguiUrlSource(type, value, mimeType);
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
