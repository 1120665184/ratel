package org.quyq.gwsu.kit.knowledge.engine;

/**
 * 长文档分析后的知识源上下文。
 */
public record AnalyzedKnowledgeSource(
        String sourceLanguage,
        String analysisDigest,
        String boundedSourceText) {
}
