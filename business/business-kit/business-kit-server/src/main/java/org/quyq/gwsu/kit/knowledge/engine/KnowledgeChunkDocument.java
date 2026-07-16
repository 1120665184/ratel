package org.quyq.gwsu.kit.knowledge.engine;

import java.time.Instant;

/**
 * ES-only 知识 Chunk 文档。
 */
public record KnowledgeChunkDocument(
        String chunkId,
        String pageId,
        String pageVersionId,
        String pageBlockId,
        String sourceDocumentId,
        String title,
        String headingPath,
        String content,
        Integer chunkOrder,
        String contentHash,
        String status,
        Integer version,
        Instant indexedAt,
        float[] embedding,
        String embeddingModel) {
}
