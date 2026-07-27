package org.quyq.gwsu.common.ai.agui.model.content;

import org.quyq.gwsu.common.ai.agui.model.part.AguiContentPart;

import java.util.Collections;
import java.util.List;

public record AguiPartsContent(List<AguiContentPart> parts) implements AguiMessageContent {

    public AguiPartsContent {
        parts = parts != null ? Collections.unmodifiableList(parts) : List.of();
    }
}
