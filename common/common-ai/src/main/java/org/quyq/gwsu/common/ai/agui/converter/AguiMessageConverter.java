package org.quyq.gwsu.common.ai.agui.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.util.JsonException;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.agui.domain.AguiFunctionCall;
import org.quyq.gwsu.common.ai.agui.domain.AguiMessage;
import org.quyq.gwsu.common.ai.agui.domain.AguiToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AguiMessageConverter {

    public Msg toMsg(AguiMessage aguiMessage) {
        MsgRole role = convertRole(aguiMessage.role());
        List<ContentBlock> blocks = new ArrayList<>();
        if (aguiMessage.content() != null && !aguiMessage.content().isEmpty()) {
            if (aguiMessage.isToolMessage() && aguiMessage.toolCallId() != null) {
                blocks.add(ToolResultBlock.of(
                        aguiMessage.toolCallId(),
                        null,
                        TextBlock.builder().text(aguiMessage.content()).build()));
            } else {
                blocks.add(TextBlock.builder().text(aguiMessage.content()).build());
            }
        }
        if (aguiMessage.hasToolCalls()) {
            for (AguiToolCall tc : aguiMessage.toolCalls()) {
                blocks.add(toToolUseBlock(tc));
            }
        }
        return Msg.builder().id(aguiMessage.id()).role(role).content(blocks).build();
    }

    public AguiMessage toAguiMessage(Msg msg) {
        String role = convertRole(msg.getRole());
        StringBuilder content = new StringBuilder();
        List<AguiToolCall> toolCalls = new ArrayList<>();
        String toolCallId = null;

        for (ContentBlock block : msg.getContent()) {
            if (block instanceof TextBlock tb) {
                if (content.length() > 0) {
                    content.append("\n");
                }
                content.append(tb.getText());
            } else if (block instanceof ToolUseBlock tub) {
                toolCalls.add(toAguiToolCall(tub));
            } else if (block instanceof ToolResultBlock trb) {
                toolCallId = trb.getId();
                for (ContentBlock output : trb.getOutput()) {
                    if (output instanceof TextBlock tb) {
                        if (content.length() > 0) {
                            content.append("\n");
                        }
                        content.append(tb.getText());
                    }
                }
            }
        }

        return new AguiMessage(msg.getId(), role, content.length() > 0 ? content.toString() : null,
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
}
