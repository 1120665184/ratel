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
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.config.properties.AssistantConfigDTO;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Enum-based strategy for creating concrete {@link Model} instances from configuration.
 */
public enum ModelProviderType {
    DASHSCOPE("dashscope") {
        @Override
        protected Model createModel(AssistantConfigDTO config) {
            AssistantConfigDTO.DashscopeConfigDTO c = config.getDashscope();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("DashScope API Key must be configured");
            }

            DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName())
                    .stream(c.isStream());

            if (c.isEnableThinking()) {
                builder.enableThinking(true);
            }

            if (Objects.nonNull(config.getGenerateOptions())) {
                builder.defaultOptions(transGenerateOptions(config.getGenerateOptions()));
            }


            return builder.build();
        }
    },
    OPENAI("openai") {
        @Override
        protected Model createModel(AssistantConfigDTO config) {
            AssistantConfigDTO.OpenaiConfigDTO c = config.getOpenai();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("OpenAI API Key must be configured");
            }

            OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName())
                    .stream(c.isStream());

            if (StringUtils.hasText(c.getBaseUrl())) {
                builder.baseUrl(c.getBaseUrl());
            }
            if (StringUtils.hasText(c.getEndpointPath())) {
                builder.endpointPath(c.getEndpointPath());
            }

            if (Objects.nonNull(config.getGenerateOptions())) {
                builder.generateOptions(transGenerateOptions(config.getGenerateOptions()));
            }

            return builder.build();
        }
    },
    GEMINI("gemini") {
        @Override
        protected Model createModel(AssistantConfigDTO config) {
            AssistantConfigDTO.GeminiConfigDTO c = config.getGemini();
            if (c == null) {
                throw new AgentException("Gemini config must not be null");
            }
            if (!StringUtils.hasText(c.getApiKey())
                    && !StringUtils.hasText(c.getProject())) {
                throw new AgentException(
                        "Either Gemini API Key or GCP Project must be configured");
            }

            GeminiChatModel.Builder builder = GeminiChatModel.builder()
                    .modelName(c.getModelName())
                    .streamEnabled(c.isStream());

            if (StringUtils.hasText(c.getApiKey())) {
                builder.apiKey(c.getApiKey());
            }
            if (StringUtils.hasText(c.getProject())) {
                builder.project(c.getProject());
            }
            if (StringUtils.hasText(c.getLocation())) {
                builder.location(c.getLocation());
            }

            if (Objects.nonNull(config.getGenerateOptions())) {
                builder.defaultOptions(transGenerateOptions(config.getGenerateOptions()));
            }

            return builder.build();
        }
    },
    ANTHROPIC("anthropic") {
        @Override
        protected Model createModel(AssistantConfigDTO config) {
            AssistantConfigDTO.AnthropicConfigDTO c = config.getAnthropic();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new IllegalStateException("Anthropic API Key must be configured");
            }

            AnthropicChatModel.Builder builder = AnthropicChatModel.builder()
                    .apiKey(c.getApiKey())
                    .modelName(c.getModelName())
                    .stream(c.isStream());

            if (c.getBaseUrl() != null && !c.getBaseUrl().isEmpty()) {
                builder.baseUrl(c.getBaseUrl());
            }

            if (Objects.nonNull(config.getGenerateOptions())) {
                builder.defaultOptions(transGenerateOptions(config.getGenerateOptions()));
            }


            return builder.build();
        }
    };

    private final String id;

    ModelProviderType(String id) {
        this.id = id;
    }


    protected abstract Model createModel(AssistantConfigDTO config);

    /**
     * Create a concrete {@link Model} instance from an assistant configuration DTO.
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
                return type.createModel(config);
            }
        }
        throw new IllegalStateException("Unsupported assistant config provider: " + provider);
    }


    /**
     * 转换成通用选项
     *
     * @param options
     * @return
     */
    private static GenerateOptions transGenerateOptions(AssistantConfigDTO.GenerateOptionsDTO options) {
        GenerateOptions.Builder builder = GenerateOptions.builder()
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .maxTokens(options.getMaxTokens())
                .frequencyPenalty(options.getFrequencyPenalty())
                .presencePenalty(options.getPresencePenalty())
                .topK(options.getTopK())
                .seed(options.getSeed());

        // 合并自定义请求体参数
        Map<String, Object> additionalBodyParams = options.getAdditionalBodyParams();
        if (additionalBodyParams != null && !additionalBodyParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : additionalBodyParams.entrySet()) {
                builder.additionalBodyParam(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }


}
