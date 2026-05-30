/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quyq.gwsu.common.ai.model;

import io.agentscope.core.model.*;
import org.quyq.gwsu.common.ai.config.properties.*;

import java.util.Locale;

/**
 * Enum-based strategy for creating concrete {@link Model} instances from configuration.
 */
public enum ModelProviderType {
    DASHSCOPE("dashscope") {
        @Override
        public Model createModel(AgentscopeProperties properties) {
            DashscopeProperties dashscope = properties.getDashscope();
            if (!dashscope.isEnabled()) {
                throw new IllegalStateException(
                        "DashScope model auto-configuration is disabled but selected as provider");
            }
            if (dashscope.getApiKey() == null || dashscope.getApiKey().isEmpty()) {
                throw new IllegalStateException(
                        "agentscope.dashscope.api-key must be configured when Dashscope"
                                + " auto-configuration is enabled");
            }

            DashScopeChatModel.Builder builder =
                    DashScopeChatModel.builder()
                            .apiKey(dashscope.getApiKey())
                            .modelName(dashscope.getModelName())
                            .stream(dashscope.isStream());

            if (dashscope.getEnableThinking() != null) {
                builder.enableThinking(dashscope.getEnableThinking());
            }

            return builder.build();
        }

        @Override
        protected Model doCreateFromConfig(AssistantConfigDTO config) {
            AssistantConfigDTO.DashscopeConfigDTO c = config.getDashscope();
            if (c == null || c.getApiKey() == null || c.getApiKey().isEmpty()) {
                throw new IllegalStateException("DashScope API Key must be configured");
            }

            DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName())
                    .stream(c.isStream());

            if (c.isEnableThinking()) {
                builder.enableThinking(true);
            }

            return builder.build();
        }
    },
    OPENAI("openai") {
        @Override
        public Model createModel(AgentscopeProperties properties) {
            OpenAIProperties openai = properties.getOpenai();
            if (!openai.isEnabled()) {
                throw new IllegalStateException(
                        "OpenAI model auto-configuration is disabled but selected as provider");
            }
            if (openai.getApiKey() == null || openai.getApiKey().isEmpty()) {
                throw new IllegalStateException(
                        "agentscope.openai.api-key must be configured when OpenAI provider is"
                                + " selected");
            }

            OpenAIChatModel.Builder builder =
                    OpenAIChatModel.builder()
                            .apiKey(openai.getApiKey())
                            .modelName(openai.getModelName())
                            .stream(openai.isStream());

            if (openai.getBaseUrl() != null && !openai.getBaseUrl().isEmpty()) {
                builder.baseUrl(openai.getBaseUrl());
            }

            if (openai.getEndpointPath() != null && !openai.getEndpointPath().isEmpty()) {
                builder.endpointPath(openai.getEndpointPath());
            }

            return builder.build();
        }

        @Override
        protected Model doCreateFromConfig(AssistantConfigDTO config) {
            AssistantConfigDTO.OpenaiConfigDTO c = config.getOpenai();
            if (c == null || c.getApiKey() == null || c.getApiKey().isEmpty()) {
                throw new IllegalStateException("OpenAI API Key must be configured");
            }

            OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName())
                    .stream(c.isStream());

            if (c.getBaseUrl() != null && !c.getBaseUrl().isEmpty()) {
                builder.baseUrl(c.getBaseUrl());
            }
            if (c.getEndpointPath() != null && !c.getEndpointPath().isEmpty()) {
                builder.endpointPath(c.getEndpointPath());
            }

            return builder.build();
        }
    },
    GEMINI("gemini") {
        @Override
        public Model createModel(AgentscopeProperties properties) {
            GeminiProperties gemini = properties.getGemini();
            if (!gemini.isEnabled()) {
                throw new IllegalStateException(
                        "Gemini model auto-configuration is disabled but selected as provider");
            }
            if ((gemini.getApiKey() == null || gemini.getApiKey().isEmpty())
                    && (gemini.getProject() == null || gemini.getProject().isEmpty())) {
                throw new IllegalStateException(
                        "Either agentscope.gemini.api-key or agentscope.gemini.project must be"
                                + " configured when Gemini provider is selected");
            }

            GeminiChatModel.Builder builder =
                    GeminiChatModel.builder()
                            .apiKey(gemini.getApiKey())
                            .modelName(gemini.getModelName())
                            .streamEnabled(gemini.isStream())
                            .project(gemini.getProject())
                            .location(gemini.getLocation());

            if (gemini.getVertexAI() != null) {
                builder.vertexAI(gemini.getVertexAI());
            }

            return builder.build();
        }

        @Override
        protected Model doCreateFromConfig(AssistantConfigDTO config) {
            AssistantConfigDTO.GeminiConfigDTO c = config.getGemini();
            if (c == null) {
                throw new IllegalStateException("Gemini config must not be null");
            }
            if ((c.getApiKey() == null || c.getApiKey().isEmpty())
                    && (c.getProject() == null || c.getProject().isEmpty())) {
                throw new IllegalStateException(
                        "Either Gemini API Key or GCP Project must be configured");
            }

            GeminiChatModel.Builder builder = GeminiChatModel.builder()
                    .modelName(c.getModelName())
                    .streamEnabled(c.isStream());

            if (c.getApiKey() != null && !c.getApiKey().isEmpty()) {
                builder.apiKey(c.getApiKey());
            }
            if (c.getProject() != null && !c.getProject().isEmpty()) {
                builder.project(c.getProject());
            }
            if (c.getLocation() != null && !c.getLocation().isEmpty()) {
                builder.location(c.getLocation());
            }

            return builder.build();
        }
    },
    ANTHROPIC("anthropic") {
        @Override
        public Model createModel(AgentscopeProperties properties) {
            AnthropicProperties anthropic = properties.getAnthropic();
            if (!anthropic.isEnabled()) {
                throw new IllegalStateException(
                        "Anthropic model auto-configuration is disabled but selected as provider");
            }
            if (anthropic.getApiKey() == null || anthropic.getApiKey().isEmpty()) {
                throw new IllegalStateException(
                        "agentscope.anthropic.api-key must be configured when Anthropic provider is"
                                + " selected");
            }

            AnthropicChatModel.Builder builder =
                    AnthropicChatModel.builder()
                            .apiKey(anthropic.getApiKey())
                            .modelName(anthropic.getModelName())
                            .stream(anthropic.isStream());

            if (anthropic.getBaseUrl() != null && !anthropic.getBaseUrl().isEmpty()) {
                builder.baseUrl(anthropic.getBaseUrl());
            }

            return builder.build();
        }

        @Override
        protected Model doCreateFromConfig(AssistantConfigDTO config) {
            AssistantConfigDTO.AnthropicConfigDTO c = config.getAnthropic();
            if (c == null || c.getApiKey() == null || c.getApiKey().isEmpty()) {
                throw new IllegalStateException("Anthropic API Key must be configured");
            }

            AnthropicChatModel.Builder builder = AnthropicChatModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName())
                    .stream(c.isStream());

            if (c.getBaseUrl() != null && !c.getBaseUrl().isEmpty()) {
                builder.baseUrl(c.getBaseUrl());
            }

            return builder.build();
        }
    };

    private final String id;

    ModelProviderType(String id) {
        this.id = id;
    }

    /**
     * Create a concrete {@link Model} instance using the given properties.
     */
    public abstract Model createModel(AgentscopeProperties properties);

    protected abstract Model doCreateFromConfig(AssistantConfigDTO config);

    /**
     * Create a concrete {@link Model} instance from an assistant configuration DTO.
     *
     * <p>Unlike {@link #createModel(AgentscopeProperties)} which reads from Spring configuration,
     * this method creates a Model from the user-configured assistant settings stored in the
     * database config table.
     *
     * @param config the assistant configuration DTO
     * @return a new Model instance
     */
    public static Model createModelFromConfig(AssistantConfigDTO config) {
        if (config == null || config.getProvider() == null) {
            throw new IllegalStateException("Assistant config or provider must not be null");
        }

        String provider = config.getProvider().trim().toLowerCase(Locale.ROOT);
        for (ModelProviderType type : values()) {
            if (type.id.equals(provider)) {
                return type.doCreateFromConfig(config);
            }
        }
        throw new IllegalStateException("Unsupported assistant config provider: " + provider);
    }

    /**
     * Resolve provider from root properties. Defaults to {@link #DASHSCOPE} when provider is not
     * configured.
     *
     * @param properties root configuration properties
     * @return resolved provider enum
     */
    public static ModelProviderType fromProperties(AgentscopeProperties properties) {
        ModelProperties modelProps = properties.getModel();
        String provider = modelProps != null ? modelProps.getProvider() : null;
        String normalized =
                provider == null || provider.isBlank()
                        ? OPENAI.id
                        : provider.trim().toLowerCase(Locale.ROOT);

        for (ModelProviderType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalStateException("Unsupported agentscope.model.provider: " + normalized);
    }
}
