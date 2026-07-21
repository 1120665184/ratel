package org.quyq.gwsu.kit.knowledge.engine;

import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;

/**
 * 可供模型合并计划引用的 Block。
 *
 * @param refId 稳定引用 ID
 * @param block Block 数据
 * @param sourceRef 来源引用，可为空
 */
public record KnowledgePageMergeBlockRef(
        String refId,
        KitKnowledgePageBlock block,
        KitKnowledgePageSourceRef sourceRef) {
}
