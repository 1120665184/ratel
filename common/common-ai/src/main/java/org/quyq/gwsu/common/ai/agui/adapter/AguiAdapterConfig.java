package org.quyq.gwsu.common.ai.agui.adapter;

import org.quyq.gwsu.common.ai.agui.domain.ToolMergeMode;

import java.time.Duration;

/**
 * AG-UI 适配器配置。
 *
 * @author Quyq
 * @date 2026/7/27
 */
public class AguiAdapterConfig {

    private final ToolMergeMode toolMergeMode;
    private final boolean emitStateEvents;
    private final boolean emitToolCallArgs;
    private final boolean enableReasoning;
    private final Duration runTimeout;
    private final String defaultAgentId;

    private AguiAdapterConfig(Builder builder) {
        this.toolMergeMode = builder.toolMergeMode;
        this.emitStateEvents = builder.emitStateEvents;
        this.emitToolCallArgs = builder.emitToolCallArgs;
        this.enableReasoning = builder.enableReasoning;
        this.runTimeout = builder.runTimeout;
        this.defaultAgentId = builder.defaultAgentId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AguiAdapterConfig defaultConfig() {
        return builder().build();
    }

    public ToolMergeMode getToolMergeMode() {
        return toolMergeMode;
    }

    public boolean isEmitStateEvents() {
        return emitStateEvents;
    }

    public boolean isEmitToolCallArgs() {
        return emitToolCallArgs;
    }

    public boolean isEnableReasoning() {
        return enableReasoning;
    }

    public Duration getRunTimeout() {
        return runTimeout;
    }

    public String getDefaultAgentId() {
        return defaultAgentId;
    }

    public static class Builder {

        private ToolMergeMode toolMergeMode = ToolMergeMode.MERGE_FRONTEND_PRIORITY;
        private boolean emitStateEvents = true;
        private boolean emitToolCallArgs = true;
        private boolean enableReasoning = false;
        private Duration runTimeout = Duration.ofMinutes(10);
        private String defaultAgentId;

        public Builder toolMergeMode(ToolMergeMode toolMergeMode) {
            this.toolMergeMode = toolMergeMode;
            return this;
        }

        public Builder emitStateEvents(boolean emitStateEvents) {
            this.emitStateEvents = emitStateEvents;
            return this;
        }

        public Builder emitToolCallArgs(boolean emitToolCallArgs) {
            this.emitToolCallArgs = emitToolCallArgs;
            return this;
        }

        public Builder enableReasoning(boolean enableReasoning) {
            this.enableReasoning = enableReasoning;
            return this;
        }

        public Builder runTimeout(Duration runTimeout) {
            this.runTimeout = runTimeout;
            return this;
        }

        public Builder defaultAgentId(String defaultAgentId) {
            this.defaultAgentId = defaultAgentId;
            return this;
        }

        public AguiAdapterConfig build() {
            return new AguiAdapterConfig(this);
        }
    }
}
