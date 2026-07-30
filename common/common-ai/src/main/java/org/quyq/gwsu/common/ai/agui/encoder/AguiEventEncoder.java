package org.quyq.gwsu.common.ai.agui.encoder;

import io.agentscope.core.util.JsonException;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.agui.AguiException;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;

/**
 * AG-UI 事件编码器。
 *
 * @author Quyq
 * @date 2026/7/27
 */
public class AguiEventEncoder {

    public String encode(AguiEvent event) {
        try {
            return "data: " + JsonUtils.getJsonCodec().toJson(event) + "\n\n";
        } catch (JsonException e) {
            throw new AguiException.EncodingException("Failed to encode AG-UI event", e);
        }
    }

    public String encodeToJson(AguiEvent event) {
        try {
            return " " + JsonUtils.getJsonCodec().toJson(event);
        } catch (JsonException e) {
            throw new AguiException.EncodingException("Failed to encode AG-UI event to JSON", e);
        }
    }

    public String encodeComment(String comment) {
        return ": " + comment + "\n\n";
    }

    public String keepAlive() {
        return ": keep-alive\n\n";
    }
}
