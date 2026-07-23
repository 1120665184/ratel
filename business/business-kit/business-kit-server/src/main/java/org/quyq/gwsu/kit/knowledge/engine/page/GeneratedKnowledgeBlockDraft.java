package org.quyq.gwsu.kit.knowledge.engine.page;

import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;

/**
 * 高保真生成的块草稿。
 */
public record GeneratedKnowledgeBlockDraft(
        KnowledgeBlockType blockType,
        String content,
        int sourceSegmentStartNo,
        int sourceSegmentEndNo,
        String sourceLocator) {
}
