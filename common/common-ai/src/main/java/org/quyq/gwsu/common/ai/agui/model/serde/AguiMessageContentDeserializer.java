package org.quyq.gwsu.common.ai.agui.model.serde;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import org.quyq.gwsu.common.ai.agui.model.content.AguiMessageContent;
import org.quyq.gwsu.common.ai.agui.model.content.AguiPartsContent;
import org.quyq.gwsu.common.ai.agui.model.content.AguiTextContent;
import org.quyq.gwsu.common.ai.agui.model.part.*;
import org.quyq.gwsu.common.ai.agui.model.source.AguiContentSource;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AguiMessageContentDeserializer extends ValueDeserializer<AguiMessageContent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AguiContentSourceDeserializer sourceDeserializer = new AguiContentSourceDeserializer();

    @Override
    public AguiMessageContent deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.readValueAsTree();
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return new AguiTextContent(node.asText());
        }
        if (node.isArray()) {
            List<AguiContentPart> parts = new ArrayList<>();
            for (JsonNode item : node) {
                parts.add(parsePart(item, p, ctxt));
            }
            return new AguiPartsContent(parts);
        }
        if (node.isObject()) {
            if (node.has("type")) {
                return new AguiPartsContent(List.of(parsePart(node, p, ctxt)));
            }
            if (node.has("parts") && node.get("parts").isArray()) {
                List<AguiContentPart> parts = new ArrayList<>();
                for (JsonNode item : node.get("parts")) {
                    parts.add(parsePart(item, p, ctxt));
                }
                return new AguiPartsContent(parts);
            }
            if (node.has("text")) {
                return new AguiTextContent(text(node.get("text")));
            }
        }
        return (AguiMessageContent) ctxt.handleUnexpectedToken(AguiMessageContent.class, p);
    }

    @SuppressWarnings("unchecked")
    private AguiContentPart parsePart(JsonNode node, JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String type = text(node.get("type"));
        if ("text".equalsIgnoreCase(type)) {
            return new AguiTextPart(type, text(node.get("text")));
        }

        AguiContentSource source = parseSource(node.get("source"), p, ctxt);
        Map<String, Object> metadata = node.has("metadata")
                ? OBJECT_MAPPER.treeToValue(node.get("metadata"), Map.class)
                : Map.of();

        return switch (type) {
            case "image" -> new AguiImagePart(type, source, metadata);
            case "audio" -> new AguiAudioPart(type, source, metadata);
            case "video" -> new AguiVideoPart(type, source, metadata);
            case "document" -> new AguiDocumentPart(type, source, metadata);
            default -> (AguiContentPart) ctxt.handleWeirdStringValue(
                    AguiContentPart.class, type, "Unsupported content part type");
        };
    }

    private AguiContentSource parseSource(JsonNode node, JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonParser sourceParser = OBJECT_MAPPER.treeAsTokens(node);
        sourceParser.nextToken();
        return sourceDeserializer.deserialize(sourceParser, ctxt);
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
