package org.quyq.gwsu.headless.graph;

import io.agentscope.core.state.AgentStateStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.headless.core.session.HeadlessPageWrapper;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.springframework.ai.chat.model.ChatResponse;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class HeadlessMessageHandlerTest {

    @Test
    void shouldForwardUnknownAguiEventWithoutSpecialHandling() {
        HeadlessMessageHandler handler = new HeadlessMessageHandler(
                "u1", Mockito.mock(AgentStateStore.class), "thread-1", false, false);

        AguiEvent.StateSnapshot event = new AguiEvent.StateSnapshot("thread-1", "run-1", Map.of("step", "x"));
        List<AguiEvent> events = collectEvents(handler, () -> {
            handler.onEvent(event, Mockito.mock(HeadlessPageWrapper.class));
            handler.complete();
        });

        assertEquals(2, events.size());
        assertEquals(event, events.get(0));
        AguiEvent.Custom completed = assertInstanceOf(AguiEvent.Custom.class, events.get(1));
        assertEquals("status", completed.name());
        assertEquals(Map.of("status", "COMPLETE"), completed.value());
    }

    @Test
    void shouldEmitDerivedQuestionTextEvent() {
        HeadlessMessageHandler handler = new HeadlessMessageHandler(
                "u1", Mockito.mock(AgentStateStore.class), "thread-1", false, false);

        AskUserQuestionTool.QuestionParam question = new AskUserQuestionTool.QuestionParam(
                "你更希望如何处理？",
                "处理方式",
                List.of(
                        new AskUserQuestionTool.QuestionOption("方案A", "继续执行"),
                        new AskUserQuestionTool.QuestionOption("方案B", "暂停")
                ),
                false
        );

        List<AguiEvent> events = collectEvents(handler, () -> {
            handler.onAskUserQuestion("thread-1", "tool-1", List.of(question), Mockito.mock(HeadlessPageWrapper.class));
            handler.complete();
        });

        AguiEvent.TextMessageContent textEvent = assertInstanceOf(AguiEvent.TextMessageContent.class, events.get(0));
        assertEquals("thread-1", textEvent.threadId());
        assertEquals("", textEvent.runId());
    }

    @Test
    void shouldUseRealRunIdAfterReceivingAguiEvent() {
        HeadlessMessageHandler handler = new HeadlessMessageHandler(
                "u1", Mockito.mock(AgentStateStore.class), "thread-1", false, false);

        List<AguiEvent> events = collectEvents(handler, () -> {
            handler.onEvent(new AguiEvent.TextMessageStart("thread-1", "run-real", "msg-1", "assistant"),
                    Mockito.mock(HeadlessPageWrapper.class));
            handler.onAskUserQuestion("thread-1", "tool-1", List.of(), Mockito.mock(HeadlessPageWrapper.class));
            handler.complete();
        });

        AguiEvent.Custom statusEvent = assertInstanceOf(AguiEvent.Custom.class, events.get(0));
        assertEquals("run-real", statusEvent.runId());
        AguiEvent.TextMessageStart textStart = assertInstanceOf(AguiEvent.TextMessageStart.class, events.get(1));
        assertEquals("run-real", textStart.runId());
        AguiEvent.TextMessageContent questionEvent = assertInstanceOf(AguiEvent.TextMessageContent.class, events.get(2));
        assertEquals("run-real", questionEvent.runId());
    }

    @Test
    void shouldEmitOriginalAndDerivedImageEventWhenScreenshotEnabled() {
        HeadlessPageWrapper wrapper = Mockito.mock(HeadlessPageWrapper.class);
        File file = new File("/tmp/mock-image.png");
        KitFileInfoVO fileInfo = new KitFileInfoVO();
        fileInfo.setFileId("file-1");
        fileInfo.setMediaType("image/png");
        when(wrapper.screenshot("#ai-output-panel")).thenReturn(file);
        when(wrapper.upload(any(File.class))).thenReturn(fileInfo);

        HeadlessMessageHandler handler = new HeadlessMessageHandler(
                "u1", Mockito.mock(AgentStateStore.class), "thread-1", true, false);
        AguiEvent.Custom outputEndEvent = new AguiEvent.Custom("thread-1", "run-1", "AGENT_OUTPUT_END", Map.of("text", "done"));

        List<AguiEvent> events = collectEvents(handler, () -> {
            handler.onEvent(outputEndEvent, wrapper);
            handler.onAgentOutput(outputEndEvent, wrapper);
            handler.complete();
        });

        assertEquals("status", assertInstanceOf(AguiEvent.Custom.class, events.get(0)).name());
        assertEquals("AGENT_OUTPUT_END", assertInstanceOf(AguiEvent.Custom.class, events.get(1)).name());
        AguiEvent.Custom imageEvent = assertInstanceOf(AguiEvent.Custom.class, events.get(2));
        assertEquals("output_image", imageEvent.name());
    }

    private List<AguiEvent> collectEvents(HeadlessMessageHandler handler, Runnable action) {
        List<AguiEvent> events = handler.asFlux()
                .map(HeadlessAguiEventBridge::fromChatResponse)
                .collectList()
                .timeout(Duration.ofSeconds(2))
                .doOnSubscribe(ignore -> action.run())
                .block();
        return events == null ? List.of() : events;
    }
}
