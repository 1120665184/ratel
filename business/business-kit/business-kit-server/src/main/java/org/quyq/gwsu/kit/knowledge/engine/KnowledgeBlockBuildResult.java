package org.quyq.gwsu.kit.knowledge.engine;

import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageSourceRef;

import java.util.List;

/**
 * Markdown Block 构建结果。
 */
public record KnowledgeBlockBuildResult(
        List<KnowledgePageBlock> blocks,
        List<KnowledgePageSourceRef> sourceRefs) {
}
