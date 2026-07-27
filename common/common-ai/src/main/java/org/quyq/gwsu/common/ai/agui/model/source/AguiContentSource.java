package org.quyq.gwsu.common.ai.agui.model.source;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import org.quyq.gwsu.common.ai.agui.model.serde.AguiContentSourceDeserializer;
import org.quyq.gwsu.common.ai.agui.model.serde.AguiContentSourceSerializer;

@JsonSerialize(using = AguiContentSourceSerializer.class)
@JsonDeserialize(using = AguiContentSourceDeserializer.class)
public sealed interface AguiContentSource permits AguiDataSource, AguiUrlSource {

    String type();

    String value();

    String mimeType();
}
