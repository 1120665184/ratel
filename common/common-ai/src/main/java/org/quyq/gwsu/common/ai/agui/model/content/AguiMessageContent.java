package org.quyq.gwsu.common.ai.agui.model.content;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import org.quyq.gwsu.common.ai.agui.model.serde.AguiMessageContentDeserializer;
import org.quyq.gwsu.common.ai.agui.model.serde.AguiMessageContentSerializer;

@JsonSerialize(using = AguiMessageContentSerializer.class)
@JsonDeserialize(using = AguiMessageContentDeserializer.class)
public sealed interface AguiMessageContent permits AguiPartsContent, AguiTextContent {
}
