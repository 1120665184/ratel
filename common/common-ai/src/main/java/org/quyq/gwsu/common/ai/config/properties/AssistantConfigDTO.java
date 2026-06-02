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

import java.util.HashMap;
import java.util.Map;

/**
 * 助手配置DTO，与前端 assistant_llm_config JSON 结构一一映射。
 *
 * <p>JSON 结构示例：
 *
 * <pre>{@code
 * {
 *   "provider": "openai",
 *   "dashscope": { ... },
 *   "openai": { ... },
 *   "gemini": { ... },
 *   "anthropic": { ... },
 *   "generateOptions": { ... }
 * }
 * }</pre>
 */
@Data
public class AssistantConfigDTO {

    /**
     * 当前激活的模型提供商标识。
     *
     * <p>支持值：dashscope、openai、gemini、anthropic
     */
    private String provider;

    /**
     * DashScope 提供商配置。
     */
    private DashscopeConfigDTO dashscope = new DashscopeConfigDTO();

    /**
     * OpenAI 提供商配置。
     */
    private OpenaiConfigDTO openai = new OpenaiConfigDTO();

    /**
     * Gemini 提供商配置。
     */
    private GeminiConfigDTO gemini = new GeminiConfigDTO();

    /**
     * Anthropic 提供商配置。
     */
    private AnthropicConfigDTO anthropic = new AnthropicConfigDTO();

    /**
     * 通用生成参数，所有提供商共享。
     */
    private GenerateOptionsDTO generateOptions = new GenerateOptionsDTO();


    @Data
    public static class AnthropicConfigDTO {

        /**
         * Anthropic API 密钥。
         */
        private String apiKey;

        /**
         * 模型名称，例如 claude-sonnet-4-5、claude-opus-4、claude-haiku-4-5。
         */
        private String modelName = "claude-sonnet-4-5-20250929";

        /**
         * 是否启用流式响应。
         */
        private boolean stream = true;

        /**
         * 自定义 API 地址（可选）。
         */
        private String baseUrl;

    }

    @Data
    public static class DashscopeConfigDTO {

        /**
         * DashScope API 密钥。
         */
        private String apiKey;

        /**
         * 模型名称，例如 qwen-plus、qwen-max、qwen-turbo、qwq-plus。
         */
        private String modelName = "qwen-plus";

        /**
         * 是否启用流式响应。
         */
        private boolean stream = true;

        /**
         * 是否启用思考模式。
         */
        private boolean enableThinking = false;

        /**
         * 是否启用搜索增强。
         */
        private boolean enableSearch = false;

        /**
         * 自定义 API 地址（可选）。
         */
        private String baseUrl;

    }

    @Data
    public static class GeminiConfigDTO {

        /**
         * Gemini API 密钥（直接 API 模式必填）。
         */
        private String apiKey;

        /**
         * 模型名称，例如 gemini-2.0-flash、gemini-2.5-flash、gemini-2.5-pro。
         */
        private String modelName = "gemini-2.0-flash";

        /**
         * 是否启用流式响应。
         */
        private boolean stream = true;

        /**
         * Google Cloud 项目 ID（Vertex AI 模式必填）。
         *
         * <p>当配置了 project 时，SDK 自动识别为 Vertex AI 模式。
         */
        private String project;

        /**
         * Google Cloud 区域（Vertex AI 模式可选）。
         */
        private String location = "us-central1";

    }

    @Data
    public static class GenerateOptionsDTO {

        /**
         * 温度，控制生成随机性。范围 0-2，值越高输出越随机。
         */
        private Double temperature;

        /**
         * 核采样参数（Top-P）。范围 0-1，考虑累计概率超过此值的最小 token 集。
         */
        private Double topP;

        /**
         * 最大生成 Token 数。
         */
        private Integer maxTokens;

        /**
         * 频率惩罚。范围 -2~2，根据 token 已出现频率进行惩罚以减少重复。
         */
        private Double frequencyPenalty;

        /**
         * 存在惩罚。范围 -2~2，对已出现的 token 进行惩罚以减少重复。
         */
        private Double presencePenalty;

        /**
         * Top-K 采样参数，限制每步只考虑概率最高的 K 个 token。
         */
        private Integer topK;

        /**
         * 随机种子，用于确定性生成。
         */
        private Long seed;

        /**
         * 自定义请求体参数，以 JSON 格式配置。
         *
         * <p>用于传递提供商特有的非标准参数，构建时会合并到 GenerateOptions 的 additionalBodyParams 中。
         * 键为参数名，值为参数值（支持嵌套对象）。
         *
         * <p>示例：
         * <pre>{@code
         * {
         *   "thinking": { "type": "enabled" },
         *   "custom_param": "value"
         * }
         * }</pre>
         */
        private Map<String, Object> additionalBodyParams = new HashMap<>();

    }

    @Data
    public static class OpenaiConfigDTO {

        /**
         * OpenAI API 密钥。
         */
        private String apiKey;

        /**
         * 模型名称，例如 gpt-4.1-mini、gpt-4.1、gpt-4o、o4-mini。
         */
        private String modelName = "gpt-4.1-mini";

        /**
         * 是否启用流式响应。
         */
        private boolean stream = true;

        /**
         * 兼容 OpenAI 的端点 Base URL（可选）。
         */
        private String baseUrl;

        /**
         * 自定义端点路径（可选）。
         *
         * <p>用于 OpenAI 兼容 API 使用非标准路径的场景，例如 "/v4/chat/completions"。
         */
        private String endpointPath;

    }

}
