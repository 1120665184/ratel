package org.quyq.gwsu.kit.knowledge.engine.page;

import java.util.List;

/**
 * 高保真知识页草稿。
 */
public record GeneratedKnowledgePageDraft(
        String title,
        List<GeneratedKnowledgeBlockDraft> blocks,
        String markdownContent) {
}
