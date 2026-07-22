package org.quyq.gwsu.kit.knowledge.engine.model;

import com.google.gson.Gson;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AgentScope 响应解析器，优先读取 structured data，失败时回退到文本 JSON 解析。
 */
@Component
public class AgentScopeResponseParser {

    private static final Gson GSON = new Gson();

    public <T> T parse(Msg message, Class<T> responseType) {
        if (message == null || responseType == null) {
            return null;
        }
        if (message.hasStructuredData()) {
            return message.getStructuredData(responseType);
        }
        String text = message.getTextContent();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = stripCodeFence(text.trim());
        String json = extractJsonObject(normalized);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return GSON.fromJson(json, responseType);
        } catch (Exception ignored) {
            return null;
        }
    }

    public String text(Msg message) {
        if (message == null) {
            return "";
        }
        return stripCodeFence(message.getTextContent());
    }

    private String stripCodeFence(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim();
        if (!normalized.startsWith("```")) {
            return normalized;
        }
        int firstLineBreak = normalized.indexOf('\n');
        if (firstLineBreak < 0) {
            return normalized.replace("```", "").trim();
        }
        String body = normalized.substring(firstLineBreak + 1);
        int closingFence = body.lastIndexOf("```");
        if (closingFence >= 0) {
            body = body.substring(0, closingFence);
        }
        return body.trim();
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return "";
        }
        return text.substring(start, end + 1);
    }
}
