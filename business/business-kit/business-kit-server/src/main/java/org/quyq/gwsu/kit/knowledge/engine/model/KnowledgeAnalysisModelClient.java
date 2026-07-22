package org.quyq.gwsu.kit.knowledge.engine.model;

/**
 * 知识源分析模型调用抽象。
 */
public interface KnowledgeAnalysisModelClient {

    String analyzeChunk(String prompt);

    String summarizeDigests(String prompt);
}
