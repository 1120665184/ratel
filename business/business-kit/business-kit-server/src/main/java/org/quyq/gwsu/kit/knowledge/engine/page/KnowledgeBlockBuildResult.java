package org.quyq.gwsu.kit.knowledge.engine.page;

import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;

import java.util.List;

/**
 * Markdown Block 构建结果。
 */
public record KnowledgeBlockBuildResult(
        List<KitKnowledgePageBlock> blocks,
        List<KitKnowledgePageSourceRef> sourceRefs) {
}
