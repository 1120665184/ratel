package org.quyq.gwsu.common.ai.agui.model.serde;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import org.quyq.gwsu.common.ai.agui.model.source.AguiContentSource;

public class AguiContentSourceSerializer extends ValueSerializer<AguiContentSource> {

    @Override
    public void serialize(AguiContentSource value, JsonGenerator gen, SerializationContext serializers)
            throws JacksonException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeStringProperty("type", value.type());
        gen.writeStringProperty("value", value.value());
        if (value.mimeType() != null) {
            gen.writeStringProperty("mimeType", value.mimeType());
        }
        gen.writeEndObject();
    }
}
