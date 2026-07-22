package org.quyq.gwsu.kit.knowledge.engine.chunk;

import com.knuddels.jtokkit.api.EncodingType;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.model.EmbeddingModelProvider;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 知识 Chunk 向量化服务。
 */
@Slf4j
@Component
public class KnowledgeChunkEmbeddingService {

    private static final double TOKEN_COUNT_RESERVE_PERCENTAGE = 0.1D;

    private final KnowledgeProperties properties;

    private final Supplier<Optional<EmbeddingModel>> embeddingModelSupplier = EmbeddingModelProvider::generateModel;

    public KnowledgeChunkEmbeddingService(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public boolean embedChunks(List<KnowledgeChunkDocument> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        try {
            Optional<EmbeddingModel> modelOptional = embeddingModelSupplier.get();
            if (modelOptional.isEmpty()) {
                log.warn("知识库未启用或未配置向量化模型，跳过文档向量化步骤");
                return false;
            }
            EmbeddingModel model = modelOptional.get();
            List<Document> documents = chunks.stream()
                    .map(chunk -> Document.builder()
                            .text(StringUtils.hasText(chunk.getContent()) ? chunk.getContent() : "")
                            .build())
                    .toList();
            List<float[]> embeddings = model.embed(documents, EmbeddingOptions.builder().build(), batchingStrategy());
            if (embeddings.size() != chunks.size()) {
                throw new BusinessException(KitErrorCode.E03011);
            }
            String embeddingModel = model.getClass().getSimpleName();
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i)
                        .setEmbedding(embeddings.get(i))
                        .setEmbeddingModel(embeddingModel);
            }
            return true;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    public Optional<float[]> embedQuery(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Optional.empty();
        }
        try {
            Optional<EmbeddingModel> modelOptional = embeddingModelSupplier.get();
            if (modelOptional.isEmpty()) {
                log.warn("知识库未启用或未配置向量化模型，查询将仅使用倒排索引检索");
                return Optional.empty();
            }
            return Optional.of(modelOptional.get().embed(keyword));
        } catch (RuntimeException ex) {
            log.warn("知识库查询向量化失败，将仅使用全文检索", ex);
            return Optional.empty();
        }
    }

    BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy(
                EncodingType.CL100K_BASE,
                Math.max(1, properties.getEmbeddingBatchTokenCount()),
                TOKEN_COUNT_RESERVE_PERCENTAGE);
    }
}
