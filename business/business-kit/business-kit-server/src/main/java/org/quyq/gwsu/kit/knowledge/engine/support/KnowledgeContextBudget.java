package org.quyq.gwsu.kit.knowledge.engine.support;

/**
 * 知识库长文档分析与生成上下文预算。
 */
public record KnowledgeContextBudget(
        int analysisChunkTokens,
        int overlapTokens,
        int generationSourceTokens,
        int digestTokens) {
}
