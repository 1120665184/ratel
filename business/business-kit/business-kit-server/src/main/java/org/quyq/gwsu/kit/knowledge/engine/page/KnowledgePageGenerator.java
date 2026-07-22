package org.quyq.gwsu.kit.knowledge.engine.page;

/**
 * 知识 Page 生成器。
 */
public interface KnowledgePageGenerator {

    GeneratedKnowledgePage generate(KnowledgePageGenerationRequest request);
}
