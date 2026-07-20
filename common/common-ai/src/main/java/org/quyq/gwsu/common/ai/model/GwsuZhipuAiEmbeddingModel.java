package org.quyq.gwsu.common.ai.model;

import org.quyq.gwsu.common.ai.AgentException;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.DefaultEmbeddingOptions;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 智谱向量模型适配器，避免直接依赖 Spring AI 已废弃待删除的 ZhiPuAI embedding 实现。
 */
public final class GwsuZhipuAiEmbeddingModel extends AbstractEmbeddingModel {

    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";

    private static final String EMBEDDINGS_URI = "/embeddings";

    private final RestClient restClient;

    private final MetadataMode metadataMode;

    private final DefaultEmbeddingOptions defaultOptions;

    public GwsuZhipuAiEmbeddingModel(String apiKey, String baseUrl, MetadataMode metadataMode, DefaultEmbeddingOptions defaultOptions) {
        if (!StringUtils.hasText(apiKey)) {
            throw new AgentException("ZhipuAI embedding API Key must be configured");
        }
        this.restClient = RestClient.builder()
                .baseUrl(resolveBaseUrl(baseUrl))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.metadataMode = metadataMode == null ? MetadataMode.EMBED : metadataMode;
        if (defaultOptions == null) {
            this.defaultOptions = new DefaultEmbeddingOptions();
        } else {
            this.defaultOptions = defaultOptions;
        }
    }

    @Override
    public float[] embed(Document document) {
        if (document == null) {
            return this.embed("");
        }
        return this.embed(getEmbeddingContent(document));
    }

    public String getEmbeddingContent(Document document) {
        if (document == null) {
            return "";
        }
        return document.getFormattedContent(this.metadataMode);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getInstructions())) {
            throw new AgentException("ZhipuAI embedding request must not be empty");
        }
        EmbeddingOptions mergedOptions = mergeOptions(request.getOptions());
        ZhipuEmbeddingResponse response = this.restClient.post()
                .uri(EMBEDDINGS_URI)
                .body(new ZhipuEmbeddingRequest(request.getInstructions(), mergedOptions.getModel(), mergedOptions.getDimensions()))
                .retrieve()
                .body(ZhipuEmbeddingResponse.class);
        if (response == null || CollectionUtils.isEmpty(response.data())) {
            throw new AgentException("ZhipuAI embedding response is empty");
        }

        List<Embedding> results = new ArrayList<>(response.data().size());
        for (int i = 0; i < response.data().size(); i++) {
            ZhipuEmbeddingItem item = response.data().get(i);
            float[] vector = toFloatArray(item.embedding());
            this.embeddingDimensions.compareAndSet(0, vector.length);
            results.add(new Embedding(vector, item.index() == null ? i : item.index()));
        }

        EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata();
        metadata.setModel(StringUtils.hasText(response.model()) ? response.model() : mergedOptions.getModel());
        if (response.usage() != null) {
            metadata.setUsage(new DefaultUsage(
                    response.usage().promptTokens(),
                    response.usage().completionTokens(),
                    response.usage().totalTokens()));
        }
        return new EmbeddingResponse(results, metadata);
    }

    private EmbeddingOptions mergeOptions(EmbeddingOptions requestOptions) {
        if (requestOptions == null) {
            return this.defaultOptions;
        }
        String model = StringUtils.hasText(requestOptions.getModel()) ? requestOptions.getModel() : this.defaultOptions.getModel();
        Integer dimensions = requestOptions.getDimensions() != null ? requestOptions.getDimensions() : this.defaultOptions.getDimensions();
        DefaultEmbeddingOptions options = new DefaultEmbeddingOptions();
        options.setModel(model);
        options.setDimensions(dimensions);
        return options;
    }

    private float[] toFloatArray(List<Double> values) {
        if (CollectionUtils.isEmpty(values)) {
            return new float[0];
        }
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = Objects.requireNonNullElse(values.get(i), 0D).floatValue();
        }
        return result;
    }

    private static String resolveBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return DEFAULT_BASE_URL;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record ZhipuEmbeddingRequest(List<String> input, String model, Integer dimensions) {
    }

    private record ZhipuEmbeddingResponse(String model, List<ZhipuEmbeddingItem> data, ZhipuUsage usage) {
    }

    private record ZhipuEmbeddingItem(Integer index, List<Double> embedding) {
    }

    private record ZhipuUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }
}
