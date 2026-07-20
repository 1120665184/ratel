package org.quyq.gwsu.common.ai.model;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingOptions;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.config.properties.ModelEmbeddingConfigDTO;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.DefaultEmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

/**
 * 向量化模型提供商工厂。
 */
public enum EmbeddingModelProviderType {
    DASHSCOPE("dashscope") {
        private static final String DOCUMENT_TEXT_TYPE = "document";

        @Override
        protected EmbeddingModel createModel(ModelEmbeddingConfigDTO config) {
            ModelEmbeddingConfigDTO.DashscopeEmbeddingConfigDTO c = config.getDashscope();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("DashScope embedding API Key must be configured");
            }
            DashScopeApi.Builder apiBuilder = DashScopeApi.builder().apiKey(c.getApiKey());
            if (StringUtils.hasText(c.getBaseUrl())) {
                apiBuilder.baseUrl(c.getBaseUrl());
            }
            DashScopeEmbeddingOptions.Builder optionsBuilder = DashScopeEmbeddingOptions.builder()
                    .model(c.getModelName())
                    .textType(DOCUMENT_TEXT_TYPE);
            Integer dimensions = positiveDimensions(c.getModelName(),c.getDimensions());
            if (dimensions != null) {
                optionsBuilder.dimensions(dimensions);
            }
            DashScopeEmbeddingOptions options = optionsBuilder.build();
            return new DashScopeEmbeddingModel(apiBuilder.build(), MetadataMode.EMBED, options);
        }
    },
    OPENAI("openai") {
        @Override
        protected EmbeddingModel createModel(ModelEmbeddingConfigDTO config) {
            ModelEmbeddingConfigDTO.OpenaiEmbeddingConfigDTO c = config.getOpenai();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("OpenAI embedding API Key must be configured");
            }
            OpenAiApi.Builder apiBuilder = OpenAiApi.builder().apiKey(c.getApiKey());
            if (StringUtils.hasText(c.getBaseUrl())) {
                apiBuilder.baseUrl(c.getBaseUrl());
            }
            OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder()
                    .model(c.getModelName());
            Integer dimensions = positiveDimensions(c.getModelName() ,c.getDimensions());
            if (dimensions != null) {
                optionsBuilder.dimensions(dimensions);
            }
            OpenAiEmbeddingOptions options = optionsBuilder.build();
            return new OpenAiEmbeddingModel(apiBuilder.build(), MetadataMode.EMBED, options);
        }
    },
    OLLAMA("ollama") {
        @Override
        protected EmbeddingModel createModel(ModelEmbeddingConfigDTO config) {
            ModelEmbeddingConfigDTO.OllamaEmbeddingConfigDTO c = config.getOllama();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getModelName())) {
                throw new AgentException("Ollama embedding model name must be configured");
            }
            OllamaApi.Builder apiBuilder = OllamaApi.builder();
            if (StringUtils.hasText(c.getBaseUrl())) {
                apiBuilder.baseUrl(c.getBaseUrl());
            }
            OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                    .model(c.getModelName())
                    .numBatch(c.getBatchSize())
                    .build();
            return OllamaEmbeddingModel.builder()
                    .ollamaApi(apiBuilder.build())
                    .defaultOptions(options)
                    .build();
        }
    },
    ZHIPUAI("zhipuai") {
        @Override
        protected EmbeddingModel createModel(ModelEmbeddingConfigDTO config) {
            ModelEmbeddingConfigDTO.ZhipuaiEmbeddingConfigDTO c = config.getZhipuai();
            if (Objects.isNull(c) || !StringUtils.hasText(c.getApiKey())) {
                throw new AgentException("ZhipuAI embedding API Key must be configured");
            }
            Integer dimensions = positiveDimensions( c.getModelName(),c.getDimensions());
            DefaultEmbeddingOptions options = new DefaultEmbeddingOptions();
            options.setModel(c.getModelName());
            options.setDimensions(dimensions);
            return new GwsuZhipuAiEmbeddingModel(c.getApiKey(), c.getBaseUrl(), MetadataMode.EMBED, options);
        }
    };

    private final String id;

    EmbeddingModelProviderType(String id) {
        this.id = id;
    }

    protected abstract EmbeddingModel createModel(ModelEmbeddingConfigDTO config);

    public static EmbeddingModel createModelFromConfig(ModelEmbeddingConfigDTO config) {
        if (config == null || config.getProvider() == null) {
            throw new IllegalStateException("Embedding config or provider must not be null");
        }
        String provider = config.getProvider().trim().toLowerCase(Locale.ROOT);
        for (EmbeddingModelProviderType type : values()) {
            if (type.id.equals(provider)) {
                return type.createModel(config);
            }
        }
        throw new IllegalStateException("Unsupported embedding config provider: " + provider);
    }

    private static Integer positiveDimensions(String modelName ,Integer dimensions) {
        if("bge-m3".equals(modelName)){
            return null;
        }
        return dimensions != null && dimensions > 0 ? dimensions : null;
    }
}
