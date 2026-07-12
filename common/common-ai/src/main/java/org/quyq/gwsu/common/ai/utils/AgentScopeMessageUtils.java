package org.quyq.gwsu.common.ai.utils;

import io.agentscope.core.agent.EventType;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AgentScopeMessageUtils {

    public static final String REASONING_CONTENT_KEY = "reasoning_content";

    private AgentScopeMessageUtils() {
    }

    public static AssistantMessage toAssistantMessage(Msg msg) {
        return toAssistantMessage(msg, EventType.AGENT_RESULT);
    }

    public static AssistantMessage toAssistantMessage(Msg msg, EventType eventType) {
        if (msg == null) {
            return null;
        }

        List<ThinkingBlock> thinkingBlocks = msg.getContentBlocks(ThinkingBlock.class);
        List<ToolUseBlock> toolUseBlocks = msg.getContentBlocks(ToolUseBlock.class);
        List<TextBlock> textBlocks = msg.getContentBlocks(TextBlock.class);

        String content = "";
        if (eventType == EventType.AGENT_RESULT) {
            content = textBlocks.stream()
                    .map(TextBlock::getText)
                    .reduce("", String::concat);
        }

        Map<String, Object> properties = new HashMap<>();
        if (!thinkingBlocks.isEmpty()) {
            String reasoning = thinkingBlocks.stream()
                    .map(ThinkingBlock::getThinking)
                    .reduce("", String::concat);
            properties.put(REASONING_CONTENT_KEY, reasoning);
        }

        AssistantMessage.Builder builder = AssistantMessage.builder().content(content);
        if (!properties.isEmpty()) {
            builder.properties(properties);
        }
        if (!toolUseBlocks.isEmpty()) {
            builder.toolCalls(toolUseBlocks.stream()
                    .map(toolUseBlock -> new AssistantMessage.ToolCall(
                            toolUseBlock.getId() == null ? "" : toolUseBlock.getId(),
                            "function",
                            toolUseBlock.getName() == null ? "" : toolUseBlock.getName(),
                            toolUseBlock.getInput() == null || toolUseBlock.getInput().isEmpty()
                                    ? "{}"
                                    : org.springframework.ai.util.json.JsonParser.toJson(toolUseBlock.getInput())))
                    .toList());
        }

        return builder.build();
    }
}
