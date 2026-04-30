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
 * Gemini provider specific settings.
 *
 * <p>Example configuration using direct Gemini API:
 *
 * <pre>{@code
 * agentscope:
 *   model:
 *     provider: gemini
 *   gemini:
 *     enabled: true
 *     api-key: ${GEMINI_API_KEY}
 *     model-name: gemini-2.0-flash
 *     stream: true
 * }</pre>
 *
 * <p>Example configuration using Vertex AI:
 *
 * <pre>{@code
 * agentscope:
 *   model:
 *     provider: gemini
 *   gemini:
 *     enabled: true
 *     project: your-gcp-project-id
 *     location: us-central1
 *     model-name: gemini-2.0-flash
 *     vertex-ai: true
 *     stream: true
 * }</pre>
 */
@Data
public class GeminiProperties {

    /**
     * Whether Gemini model auto-configuration is enabled.
     */
    private boolean enabled = true;

    /**
     * Gemini API key (for direct Gemini API usage).
     */
    private String apiKey;

    /**
     * Gemini model name, for example {@code gemini-2.0-flash}.
     */
    private String modelName = "gemini-2.0-flash";

    /**
     * Whether streaming responses are enabled.
     */
    private boolean stream = true;

    /**
     * Google Cloud project ID (for Vertex AI usage).
     */
    private String project;

    /**
     * Google Cloud location, for example {@code us-central1}.
     */
    private String location;

    /**
     * Whether to use Vertex AI (true) instead of direct Gemini API.
     */
    private Boolean vertexAI;

}
