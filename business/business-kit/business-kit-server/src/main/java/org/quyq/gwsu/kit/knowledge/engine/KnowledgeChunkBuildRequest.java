package org.quyq.gwsu.kit.knowledge.engine;

import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;

import java.util.List;

/**
 * Chunk 构建请求。
 */
public record KnowledgeChunkBuildRequest(
        String pageId,
        String title,
        KitKnowledgePageVersion pageVersion,
        List<KitKnowledgePageBlock> blocks,
        List<KitKnowledgePageSourceRef> sourceRefs) {
}
