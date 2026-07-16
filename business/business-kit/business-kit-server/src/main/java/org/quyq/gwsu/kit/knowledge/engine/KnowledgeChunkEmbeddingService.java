package org.quyq.gwsu.kit.knowledge.engine;

import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.model.EmbeddingModelProvider;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 知识 Chunk 向量化服务。
 */
@Slf4j
@Component
public class KnowledgeChunkEmbeddingService {

    public void embedChunks(List<KnowledgeChunkDocument> chunks) {
        if (CollectionUtils.isEmpty(chunks)) {
            return;
        }
        try {
            EmbeddingModel model = EmbeddingModelProvider.generateModel();
            List<String> contents = chunks.stream()
                    .map(KnowledgeChunkDocument::getContent)
                    .map(content -> StringUtils.hasText(content) ? content : "")
                    .toList();
            List<float[]> embeddings = model.embed(contents);
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
            return Optional.ofNullable(EmbeddingModelProvider.generateModel().embed(keyword));
        } catch (RuntimeException ex) {
            log.warn("知识库查询向量化失败，将仅使用全文检索", ex);
            return Optional.empty();
        }
    }
}
