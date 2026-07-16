package org.quyq.gwsu.kit.knowledge.engine;

import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;

import java.util.Collection;
import java.util.List;

/**
 * 知识 Chunk 索引仓储。
 */
public interface KnowledgeChunkIndexRepository {

    void ensureIndex();

    void replacePageVersion(String pageId, String pageVersionId, List<KnowledgeChunkDocument> chunks);

    List<KnowledgeSearchResultVO> search(String keyword, Collection<String> visibleSourceDocumentIds, int size);
}
