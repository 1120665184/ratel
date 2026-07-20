package org.quyq.gwsu.kit.knowledge.engine;

/**
 * 知识 Page 生成请求。
 */
public record KnowledgePageGenerationRequest(
        String fileName,
        String sourceLanguage,
        String analysisDigest,
        String boundedSourceText,
        String outputLanguage) {
}
