package org.quyq.gwsu.common.ai.agui.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import io.agentscope.core.message.AudioBlock;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.Source;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.URLSource;
import io.agentscope.core.message.VideoBlock;
import io.agentscope.core.util.JsonException;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.agui.model.part.AguiAudioPart;
import org.quyq.gwsu.common.ai.agui.model.part.AguiContentPart;
import org.quyq.gwsu.common.ai.agui.model.source.AguiContentSource;
import org.quyq.gwsu.common.ai.agui.model.source.AguiDataSource;
import org.quyq.gwsu.common.ai.agui.model.part.AguiDocumentPart;
import org.quyq.gwsu.common.ai.agui.model.AguiFunctionCall;
import org.quyq.gwsu.common.ai.agui.model.part.AguiImagePart;
import org.quyq.gwsu.common.ai.agui.model.AguiMessage;
import org.quyq.gwsu.common.ai.agui.model.content.AguiMessageContent;
import org.quyq.gwsu.common.ai.agui.model.content.AguiPartsContent;
import org.quyq.gwsu.common.ai.agui.model.content.AguiTextContent;
import org.quyq.gwsu.common.ai.agui.model.part.AguiTextPart;
import org.quyq.gwsu.common.ai.agui.model.AguiToolCall;
import org.quyq.gwsu.common.ai.agui.model.source.AguiUrlSource;
import org.quyq.gwsu.common.ai.agui.model.part.AguiVideoPart;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AguiMessageConverter {

    public static final String METADATA_AGUI_ORIGINAL_CONTENT = "agui_original_content";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Msg toMsg(AguiMessage aguiMessage) {
        MsgRole role = convertRole(aguiMessage.role());
        List<ContentBlock> blocks = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();
        if (aguiMessage.content() != null) {
            if (aguiMessage.isToolMessage() && aguiMessage.toolCallId() != null) {
                blocks.add(ToolResultBlock.of(
                        aguiMessage.toolCallId(),
                        null,
                        TextBlock.builder().text(Objects.toString(aguiMessage.textContent(), "")).build()));
            } else {
                blocks.addAll(toContentBlocks(aguiMessage.content()));
            }
            if (aguiMessage.content() instanceof AguiPartsContent) {
                metadata.put(METADATA_AGUI_ORIGINAL_CONTENT, aguiMessage.content());
            }
        }
        if (aguiMessage.hasToolCalls()) {
            for (AguiToolCall tc : aguiMessage.toolCalls()) {
                blocks.add(toToolUseBlock(tc));
            }
        }
        return Msg.builder()
                .id(aguiMessage.id())
                .role(role)
                .content(blocks)
                .metadata(metadata)
                .build();
    }

    public AguiMessage toAguiMessage(Msg msg) {
        String role = convertRole(msg.getRole());
        StringBuilder textContent = new StringBuilder();
        List<AguiToolCall> toolCalls = new ArrayList<>();
        String toolCallId = null;
        AguiMessageContent originalContent = readOriginalContent(msg);
        List<AguiContentPart> parts = new ArrayList<>();

        for (ContentBlock block : msg.getContent()) {
            if (block instanceof TextBlock tb) {
                if (originalContent == null) {
                    parts.add(AguiTextPart.of(tb.getText()));
                }
                if (textContent.length() > 0) {
                    textContent.append("\n");
                }
                textContent.append(tb.getText());
            } else if (block instanceof ImageBlock imageBlock) {
                parts.add(new AguiImagePart("image", fromSource(imageBlock.getSource()), Map.of()));
            } else if (block instanceof AudioBlock audioBlock) {
                parts.add(new AguiAudioPart("audio", fromSource(audioBlock.getSource()), Map.of()));
            } else if (block instanceof VideoBlock videoBlock) {
                parts.add(new AguiVideoPart("video", fromSource(videoBlock.getSource()), Map.of()));
            } else if (block instanceof ToolUseBlock tub) {
                toolCalls.add(toAguiToolCall(tub));
            } else if (block instanceof ToolResultBlock trb) {
                toolCallId = trb.getId();
                for (ContentBlock output : trb.getOutput()) {
                    if (output instanceof TextBlock tb) {
                        if (textContent.length() > 0) {
                            textContent.append("\n");
                        }
                        textContent.append(tb.getText());
                    }
                }
            }
        }

        AguiMessageContent content = originalContent != null
                ? originalContent
                : buildContentFromParts(parts, textContent.toString());

        return new AguiMessage(msg.getId(), role, content,
                toolCalls.isEmpty() ? null : toolCalls, toolCallId);
    }

    public List<Msg> toMsgList(List<AguiMessage> aguiMessages) {
        return aguiMessages.stream().map(this::toMsg).collect(Collectors.toList());
    }

    public List<AguiMessage> toAguiMessageList(List<Msg> msgs) {
        return msgs.stream().map(this::toAguiMessage).collect(Collectors.toList());
    }

    private MsgRole convertRole(String role) {
        return switch (role.toLowerCase()) {
            case "user" -> MsgRole.USER;
            case "assistant" -> MsgRole.ASSISTANT;
            case "system" -> MsgRole.SYSTEM;
            case "tool" -> MsgRole.TOOL;
            default -> MsgRole.USER;
        };
    }

    private String convertRole(MsgRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

    private ToolUseBlock toToolUseBlock(AguiToolCall tc) {
        Map<String, Object> input = parseJsonArguments(tc.function().arguments());
        return ToolUseBlock.builder().id(tc.id()).name(tc.function().name()).input(input).build();
    }

    private AguiToolCall toAguiToolCall(ToolUseBlock tub) {
        String arguments = serializeArguments(tub.getInput());
        AguiFunctionCall function = new AguiFunctionCall(tub.getName(), arguments);
        return new AguiToolCall(tub.getId(), function);
    }

    private Map<String, Object> parseJsonArguments(String arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        try {
            return JsonUtils.getJsonCodec().fromJson(arguments, new TypeReference<Map<String, Object>>() {});
        } catch (JsonException e) {
            return Map.of();
        }
    }

    private String serializeArguments(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "{}";
        }
        try {
            return JsonUtils.getJsonCodec().toJson(arguments);
        } catch (JsonException e) {
            return "{}";
        }
    }

    private List<ContentBlock> toContentBlocks(AguiMessageContent content) {
        List<ContentBlock> blocks = new ArrayList<>();
        if (content instanceof AguiTextContent textContent) {
            if (textContent.text() != null && !textContent.text().isEmpty()) {
                blocks.add(TextBlock.builder().text(textContent.text()).build());
            }
            return blocks;
        }
        if (content instanceof AguiPartsContent partsContent) {
            for (AguiContentPart part : partsContent.parts()) {
                if (part instanceof AguiTextPart textPart) {
                    if (textPart.text() != null && !textPart.text().isEmpty()) {
                        blocks.add(TextBlock.builder().text(textPart.text()).build());
                    }
                } else if (part instanceof AguiImagePart imagePart) {
                    blocks.add(ImageBlock.builder().source(toSource(imagePart.source())).build());
                } else if (part instanceof AguiAudioPart audioPart) {
                    blocks.add(AudioBlock.builder().source(toSource(audioPart.source())).build());
                } else if (part instanceof AguiVideoPart videoPart) {
                    blocks.add(VideoBlock.builder().source(toSource(videoPart.source())).build());
                } else if (part instanceof AguiDocumentPart documentPart) {
                    String documentText = toDocumentPrompt(documentPart);
                    if (!documentText.isEmpty()) {
                        blocks.add(TextBlock.builder().text(documentText).build());
                    }
                }
            }
        }
        return blocks;
    }

    private Source toSource(AguiContentSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Media source cannot be null");
        }
        if (source instanceof AguiDataSource dataSource) {
            return Base64Source.builder()
                    .mediaType(dataSource.mimeType())
                    .data(dataSource.value())
                    .build();
        }
        if (source instanceof AguiUrlSource urlSource) {
            return URLSource.builder()
                    .url(urlSource.value())
                    .build();
        }
        throw new IllegalArgumentException("Unsupported media source type: " + source.type());
    }

    private AguiContentSource fromSource(Source source) {
        if (source instanceof Base64Source base64Source) {
            return new AguiDataSource("data", base64Source.getData(), base64Source.getMediaType());
        }
        if (source instanceof URLSource urlSource) {
            return new AguiUrlSource("url", urlSource.getUrl(), null);
        }
        return null;
    }

    private String toDocumentPrompt(AguiDocumentPart documentPart) {
        AguiContentSource source = documentPart.source();
        if (source == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[document]");
        Object filename = documentPart.metadata().get("filename");
        if (filename != null) {
            builder.append(" filename=").append(filename);
        }
        if (source.mimeType() != null && !source.mimeType().isBlank()) {
            builder.append(" mimeType=").append(source.mimeType());
        }
        if ("url".equalsIgnoreCase(source.type())) {
            builder.append(" url=").append(source.value());
        } else {
            builder.append(" source=data");
        }
        return builder.toString();
    }

    private AguiMessageContent buildContentFromParts(List<AguiContentPart> parts, String mergedText) {
        if (parts.isEmpty()) {
            return mergedText == null || mergedText.isEmpty() ? null : new AguiTextContent(mergedText);
        }
        boolean containsNonText = parts.stream().anyMatch(part -> !(part instanceof AguiTextPart));
        if (!containsNonText) {
            return mergedText == null || mergedText.isEmpty() ? null : new AguiTextContent(mergedText);
        }
        return new AguiPartsContent(parts);
    }

    private AguiMessageContent readOriginalContent(Msg msg) {
        if (msg.getMetadata() == null) {
            return null;
        }
        Object raw = msg.getMetadata().get(METADATA_AGUI_ORIGINAL_CONTENT);
        if (raw == null) {
            return null;
        }
        if (raw instanceof AguiMessageContent aguiMessageContent) {
            return aguiMessageContent;
        }
        return OBJECT_MAPPER.convertValue(raw, AguiMessageContent.class);
    }
}
