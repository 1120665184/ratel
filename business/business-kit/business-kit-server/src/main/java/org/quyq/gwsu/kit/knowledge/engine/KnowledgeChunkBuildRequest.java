package org.quyq.gwsu.kit.knowledge.engine;

import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageVersion;

import java.util.List;

/**
 * Chunk 构建请求。
 */
public record KnowledgeChunkBuildRequest(
        String pageId,
        String title,
        KnowledgePageVersion pageVersion,
        List<KnowledgePageBlock> blocks,
        List<KnowledgePageSourceRef> sourceRefs) {
}
