package org.quyq.gwsu.common.ai.agui.model.serde;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import org.quyq.gwsu.common.ai.agui.model.content.AguiMessageContent;
import org.quyq.gwsu.common.ai.agui.model.content.AguiPartsContent;
import org.quyq.gwsu.common.ai.agui.model.content.AguiTextContent;

public class AguiMessageContentSerializer extends ValueSerializer<AguiMessageContent> {

    @Override
    public void serialize(AguiMessageContent value, JsonGenerator gen, SerializationContext serializers)
            throws JacksonException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value instanceof AguiTextContent textContent) {
            gen.writeString(textContent.text());
            return;
        }
        if (value instanceof AguiPartsContent partsContent) {
            gen.writePOJO(partsContent.parts());
            return;
        }
        gen.writeNull();
    }
}
