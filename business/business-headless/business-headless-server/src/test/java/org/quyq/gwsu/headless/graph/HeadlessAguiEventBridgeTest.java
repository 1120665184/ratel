package org.quyq.gwsu.headless.graph;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class HeadlessAguiEventBridgeTest {

    @Test
    void shouldRoundTripCustomEventThroughChatResponse() {
        AguiEvent.Custom event = new AguiEvent.Custom("thread-1", "run-1", "status", Map.of("status", "OUTPUTTING"));

        ChatResponse response = HeadlessAguiEventBridge.toChatResponse(event);

        AguiEvent decoded = HeadlessAguiEventBridge.fromChatResponse(response);
        AguiEvent.Custom custom = assertInstanceOf(AguiEvent.Custom.class, decoded);
        assertEquals("thread-1", custom.threadId());
        assertEquals("run-1", custom.runId());
        assertEquals("status", custom.name());
        assertEquals(Map.of("status", "OUTPUTTING"), custom.value());
    }

    @Test
    void shouldRoundTripRawEventThroughChatResponse() {
        AguiEvent.Raw event = new AguiEvent.Raw("thread-2", "run-2", Map.of("code", "E01", "message", "boom"));

        ChatResponse response = HeadlessAguiEventBridge.toChatResponse(event);

        AguiEvent decoded = HeadlessAguiEventBridge.fromChatResponse(response);
        AguiEvent.Raw raw = assertInstanceOf(AguiEvent.Raw.class, decoded);
        assertEquals("thread-2", raw.threadId());
        assertEquals("run-2", raw.runId());
        assertEquals(Map.of("code", "E01", "message", "boom"), raw.rawEvent());
    }
}
