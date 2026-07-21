package org.quyq.gwsu.kit.knowledge.engine;

import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 知识 Chunk 索引仓储。
 */
public interface KnowledgeChunkIndexRepository {

    void ensureIndex();

    void replacePageVersion(String pageId, String pageVersionId, List<KnowledgeChunkDocument> chunks);

    void deleteBySourceDocumentId(String sourceDocumentId);

    List<KnowledgeSearchResultVO> search(
            String keyword,
            Collection<String> visibleSourceDocumentIds,
            int size,
            Optional<float[]> queryEmbedding);

    Optional<KnowledgeSearchResultVO> findAdjacentChunk(
            String chunkId,
            KnowledgeChunkDirection direction,
            Collection<String> visibleSourceDocumentIds);
}
