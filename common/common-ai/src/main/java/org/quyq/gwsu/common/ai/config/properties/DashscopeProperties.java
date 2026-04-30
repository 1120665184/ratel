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
 * DashScope provider specific configuration properties.
 *
 * <p>Example configuration (default provider when {@code agentscope.model.provider} is omitted):
 *
 * <pre>{@code
 * agentscope:
 *   dashscope:
 *     enabled: true
 *     api-key: ${DASHSCOPE_API_KEY}
 *     model-name: qwen-plus
 *     stream: true
 *     enable-thinking: true
 * }</pre>
 */
@Data
public class DashscopeProperties {

    /**
     * Whether to enable DashScope model auto-configuration.
     */
    private boolean enabled = true;

    /**
     * DashScope API key used to authenticate requests.
     */
    private String apiKey;

    /**
     * DashScope model name, for example {@code qwen-plus} or {@code qwen-max}.
     */
    private String modelName = "qwen-plus";

    /**
     * Whether to enable streaming responses.
     */
    private boolean stream = true;

    /**
     * Whether to enable thinking mode (optional).
     */
    private Boolean enableThinking;

}
