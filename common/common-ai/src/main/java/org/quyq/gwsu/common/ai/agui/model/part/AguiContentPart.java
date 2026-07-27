package org.quyq.gwsu.common.ai.agui.model.part;

public sealed interface AguiContentPart permits
        AguiAudioPart,
        AguiDocumentPart,
        AguiImagePart,
        AguiTextPart,
        AguiVideoPart {

    String type();
}
