package org.quyq.gwsu.kit.knowledge.engine.page;

/**
 * 知识 Page 生成请求。
 */
public record KnowledgePageGenerationRequest(
        String fileName,
        String sourceLanguage,
        String analysisDigest,
        String sourceContext,
        String outputLanguage) {
}
