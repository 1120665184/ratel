package org.quyq.gwsu.kit.knowledge.engine;

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

    private final Supplier<EmbeddingModel> embeddingModelSupplier = EmbeddingModelProvider::generateModel;

    public KnowledgeChunkEmbeddingService(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public void embedChunks(List<KnowledgeChunkDocument> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        try {
            EmbeddingModel model = embeddingModelSupplier.get();
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
            return Optional.of(embeddingModelSupplier.get().embed(keyword));
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
