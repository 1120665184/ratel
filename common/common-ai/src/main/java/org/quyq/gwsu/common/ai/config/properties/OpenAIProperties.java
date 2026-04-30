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
package org.quyq.gwsu.common.ai.config.properties;

import lombok.Data;

/**
 * OpenAI provider specific settings.
 *
 * <p>Example configuration:
 *
 * <pre>{@code
 * agentscope:
 *   model:
 *     provider: openai
 *   openai:
 *     enabled: true
 *     api-key: ${OPENAI_API_KEY}
 *     model-name: gpt-4.1-mini
 *     # base-url: https://api.openai.com/v1 # optional, for compatible endpoints
 *     # endpoint-path: /v1/chat/completions # optional, for custom endpoint paths
 *     stream: true
 * }</pre>
 */
@Data
public class OpenAIProperties {

    /**
     * Whether OpenAI model auto-configuration is enabled.
     */
    private boolean enabled = true;

    /**
     * OpenAI API key.
     */
    private String apiKey;

    /**
     * OpenAI model name, for example {@code gpt-4.1-mini}.
     */
    private String modelName = "gpt-4.1-mini";

    /**
     * Optional base URL for compatible OpenAI endpoints.
     */
    private String baseUrl;

    /**
     * Optional endpoint path for compatible OpenAI endpoints.
     * <p>Allows customization for OpenAI-compatible APIs that use different
     * endpoint paths than the standard OpenAI API (e.g., "/v4/chat/completions").
     */
    private String endpointPath;

    /**
     * Whether streaming responses are enabled.
     */
    private boolean stream = true;

}
