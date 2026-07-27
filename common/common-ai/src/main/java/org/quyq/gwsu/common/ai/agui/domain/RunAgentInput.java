package org.quyq.gwsu.common.ai.agui.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RunAgentInput(String threadId, String runId, List<AguiMessage> messages, List<AguiTool> tools,
                            List<AguiContext> context, Map<String, Object> state, Map<String, Object> forwardedProps) {

    @JsonCreator
    public RunAgentInput(@JsonProperty("threadId") String threadId, @JsonProperty("runId") String runId,
                         @JsonProperty("messages") List<AguiMessage> messages, @JsonProperty("tools") List<AguiTool> tools,
                         @JsonProperty("context") List<AguiContext> context, @JsonProperty("state") Map<String, Object> state,
                         @JsonProperty("forwardedProps") Map<String, Object> forwardedProps) {
        this.threadId = Objects.requireNonNull(threadId, "threadId cannot be null");
        this.runId = Objects.requireNonNull(runId, "runId cannot be null");
        this.messages = messages != null ? Collections.unmodifiableList(messages) : Collections.emptyList();
        this.tools = tools != null ? Collections.unmodifiableList(tools) : Collections.emptyList();
        this.context = context != null ? Collections.unmodifiableList(context) : Collections.emptyList();
        this.state = state != null ? Collections.unmodifiableMap(new HashMap<>(state)) : Collections.emptyMap();
        this.forwardedProps = forwardedProps != null ? Collections.unmodifiableMap(new HashMap<>(forwardedProps)) : Collections.emptyMap();
    }

    public Object getForwardedProp(String key) {
        return forwardedProps.get(key);
    }

    public Object getForwardedProp(String key, Object defaultValue) {
        return forwardedProps.getOrDefault(key, defaultValue);
    }

    public boolean hasMessages() {
        return messages != null && !messages.isEmpty();
    }

    public boolean hasTools() {
        return tools != null && !tools.isEmpty();
    }

    public boolean hasContext() {
        return context != null && !context.isEmpty();
    }

    public boolean hasState() {
        return state != null && !state.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String threadId;
        private String runId;
        private List<AguiMessage> messages;
        private List<AguiTool> tools;
        private List<AguiContext> context;
        private Map<String, Object> state;
        private Map<String, Object> forwardedProps;

        public Builder threadId(String threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder messages(List<AguiMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder tools(List<AguiTool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder context(List<AguiContext> context) {
            this.context = context;
            return this;
        }

        public Builder state(Map<String, Object> state) {
            this.state = state;
            return this;
        }

        public Builder forwardedProps(Map<String, Object> forwardedProps) {
            this.forwardedProps = forwardedProps;
            return this;
        }

        public RunAgentInput build() {
            return new RunAgentInput(threadId, runId, messages, tools, context, state, forwardedProps);
        }
    }
}
