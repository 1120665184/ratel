package org.quyq.gwsu.common.ai.agui.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AguiEvent.RunStarted.class, name = "RUN_STARTED"),
        @JsonSubTypes.Type(value = AguiEvent.RunFinished.class, name = "RUN_FINISHED"),
        @JsonSubTypes.Type(value = AguiEvent.TextMessageStart.class, name = "TEXT_MESSAGE_START"),
        @JsonSubTypes.Type(value = AguiEvent.TextMessageContent.class, name = "TEXT_MESSAGE_CONTENT"),
        @JsonSubTypes.Type(value = AguiEvent.TextMessageEnd.class, name = "TEXT_MESSAGE_END"),
        @JsonSubTypes.Type(value = AguiEvent.ToolCallStart.class, name = "TOOL_CALL_START"),
        @JsonSubTypes.Type(value = AguiEvent.ToolCallArgs.class, name = "TOOL_CALL_ARGS"),
        @JsonSubTypes.Type(value = AguiEvent.ToolCallEnd.class, name = "TOOL_CALL_END"),
        @JsonSubTypes.Type(value = AguiEvent.ToolCallResult.class, name = "TOOL_CALL_RESULT"),
        @JsonSubTypes.Type(value = AguiEvent.StateSnapshot.class, name = "STATE_SNAPSHOT"),
        @JsonSubTypes.Type(value = AguiEvent.StateDelta.class, name = "STATE_DELTA"),
        @JsonSubTypes.Type(value = AguiEvent.Raw.class, name = "RAW"),
        @JsonSubTypes.Type(value = AguiEvent.Custom.class, name = "CUSTOM"),
        @JsonSubTypes.Type(value = AguiEvent.ReasoningStart.class, name = "REASONING_START"),
        @JsonSubTypes.Type(value = AguiEvent.ReasoningMessageStart.class, name = "REASONING_MESSAGE_START"),
        @JsonSubTypes.Type(value = AguiEvent.ReasoningMessageContent.class, name = "REASONING_MESSAGE_CONTENT"),
        @JsonSubTypes.Type(value = AguiEvent.ReasoningMessageEnd.class, name = "REASONING_MESSAGE_END"),
        @JsonSubTypes.Type(value = AguiEvent.ReasoningMessageChunk.class, name = "REASONING_MESSAGE_CHUNK"),
        @JsonSubTypes.Type(value = AguiEvent.ReasoningEnd.class, name = "REASONING_END")
})
public sealed interface AguiEvent permits
        AguiEvent.RunStarted,
        AguiEvent.RunFinished,
        AguiEvent.TextMessageStart,
        AguiEvent.TextMessageContent,
        AguiEvent.TextMessageEnd,
        AguiEvent.ToolCallStart,
        AguiEvent.ToolCallArgs,
        AguiEvent.ToolCallEnd,
        AguiEvent.ToolCallResult,
        AguiEvent.StateSnapshot,
        AguiEvent.StateDelta,
        AguiEvent.Raw,
        AguiEvent.Custom,
        AguiEvent.ReasoningStart,
        AguiEvent.ReasoningMessageStart,
        AguiEvent.ReasoningMessageContent,
        AguiEvent.ReasoningMessageEnd,
        AguiEvent.ReasoningMessageChunk,
        AguiEvent.ReasoningEnd {

    @JsonIgnore
    AguiEventType getType();

    String getThreadId();

    String getRunId();

    record RunStarted(String threadId, String runId) implements AguiEvent {
        @JsonCreator
        public RunStarted(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.RUN_STARTED; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record RunFinished(String threadId, String runId) implements AguiEvent {
        @JsonCreator
        public RunFinished(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.RUN_FINISHED; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record TextMessageStart(String threadId, String runId, String messageId, String role) implements AguiEvent {
        @JsonCreator
        public TextMessageStart(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                                @JsonProperty("messageId") String messageId, @JsonProperty("role") String role) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
            this.role = Objects.requireNonNull(role, "role cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.TEXT_MESSAGE_START; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record TextMessageContent(String threadId, String runId, String messageId, String delta) implements AguiEvent {
        @JsonCreator
        public TextMessageContent(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                                  @JsonProperty("messageId") String messageId, @JsonProperty("delta") String delta) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
            this.delta = Objects.requireNonNull(delta, "delta cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.TEXT_MESSAGE_CONTENT; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record TextMessageEnd(String threadId, String runId, String messageId) implements AguiEvent {
        @JsonCreator
        public TextMessageEnd(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                              @JsonProperty("messageId") String messageId) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.TEXT_MESSAGE_END; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ToolCallStart(String threadId, String runId, String toolCallId, String toolCallName) implements AguiEvent {
        @JsonCreator
        public ToolCallStart(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                             @JsonProperty("toolCallId") String toolCallId, @JsonProperty("toolCallName") String toolCallName) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.toolCallId = Objects.requireNonNull(toolCallId, "toolCallId cannot be null");
            this.toolCallName = Objects.requireNonNull(toolCallName, "toolCallName cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.TOOL_CALL_START; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ToolCallArgs(String threadId, String runId, String toolCallId, String delta) implements AguiEvent {
        @JsonCreator
        public ToolCallArgs(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                            @JsonProperty("toolCallId") String toolCallId, @JsonProperty("delta") String delta) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.toolCallId = Objects.requireNonNull(toolCallId, "toolCallId cannot be null");
            this.delta = Objects.requireNonNull(delta, "delta cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.TOOL_CALL_ARGS; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ToolCallEnd(String threadId, String runId, String toolCallId) implements AguiEvent {
        @JsonCreator
        public ToolCallEnd(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                           @JsonProperty("toolCallId") String toolCallId) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.toolCallId = Objects.requireNonNull(toolCallId, "toolCallId cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.TOOL_CALL_END; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ToolCallResult(String threadId, String runId, String toolCallId, Object content, String role, String messageId) implements AguiEvent {
        @JsonCreator
        public ToolCallResult(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                              @JsonProperty("toolCallId") String toolCallId, @JsonProperty("content") Object content,
                              @JsonProperty("role") String role, @JsonProperty("messageId") String messageId) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.toolCallId = Objects.requireNonNull(toolCallId, "toolCallId cannot be null");
            this.content = content;
            this.role = Objects.requireNonNull(role, "role cannot be null");
            this.messageId = messageId;
        }
        @Override public AguiEventType getType() { return AguiEventType.TOOL_CALL_RESULT; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record StateSnapshot(String threadId, String runId, Map<String, Object> snapshot) implements AguiEvent {
        @JsonCreator
        public StateSnapshot(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                             @JsonProperty("snapshot") Map<String, Object> snapshot) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.snapshot = snapshot != null ? Collections.unmodifiableMap(new HashMap<>(snapshot)) : Collections.emptyMap();
        }
        @Override public AguiEventType getType() { return AguiEventType.STATE_SNAPSHOT; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record StateDelta(String threadId, String runId, List<Map<String, Object>> delta) implements AguiEvent {
        @JsonCreator
        public StateDelta(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                          @JsonProperty("delta") List<Map<String, Object>> delta) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.delta = delta != null ? Collections.unmodifiableList(delta) : Collections.emptyList();
        }
        @Override public AguiEventType getType() { return AguiEventType.STATE_DELTA; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record Raw(String threadId, String runId, Object rawEvent) implements AguiEvent {
        @JsonCreator
        public Raw(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                   @JsonProperty("rawEvent") Object rawEvent) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.rawEvent = rawEvent;
        }
        @Override public AguiEventType getType() { return AguiEventType.RAW; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record Custom(String threadId, String runId, String name, Object value) implements AguiEvent {
        @JsonCreator
        public Custom(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                      @JsonProperty("name") String name, @JsonProperty("value") Object value) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.value = value;
        }
        @Override public AguiEventType getType() { return AguiEventType.CUSTOM; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ReasoningStart(String threadId, String runId) implements AguiEvent {
        @JsonCreator
        public ReasoningStart(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.REASONING_START; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ReasoningMessageStart(String threadId, String runId, String messageId, String role) implements AguiEvent {
        @JsonCreator
        public ReasoningMessageStart(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                                     @JsonProperty("messageId") String messageId, @JsonProperty("role") String role) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
            this.role = Objects.requireNonNull(role, "role cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.REASONING_MESSAGE_START; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ReasoningMessageContent(String threadId, String runId, String messageId, String delta) implements AguiEvent {
        @JsonCreator
        public ReasoningMessageContent(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                                       @JsonProperty("messageId") String messageId, @JsonProperty("delta") String delta) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
            this.delta = Objects.requireNonNull(delta, "delta cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.REASONING_MESSAGE_CONTENT; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ReasoningMessageEnd(String threadId, String runId, String messageId) implements AguiEvent {
        @JsonCreator
        public ReasoningMessageEnd(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                                   @JsonProperty("messageId") String messageId) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.REASONING_MESSAGE_END; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ReasoningMessageChunk(String threadId, String runId, String messageId, String delta) implements AguiEvent {
        @JsonCreator
        public ReasoningMessageChunk(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                                     @JsonProperty("messageId") String messageId, @JsonProperty("delta") String delta) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
            this.delta = Objects.requireNonNull(delta, "delta cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.REASONING_MESSAGE_CHUNK; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }

    record ReasoningEnd(String threadId, String runId) implements AguiEvent {
        @JsonCreator
        public ReasoningEnd(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId) {
            this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
            this.runId = Objects.requireNonNull(runId, "runId cannot be null");
        }
        @Override public AguiEventType getType() { return AguiEventType.REASONING_END; }
        @Override public String getThreadId() { return threadId; }
        @Override public String getRunId() { return runId; }
    }
}
