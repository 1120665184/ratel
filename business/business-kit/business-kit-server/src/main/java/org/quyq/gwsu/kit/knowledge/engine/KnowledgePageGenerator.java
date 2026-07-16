package org.quyq.gwsu.kit.knowledge.engine;

/**
 * 知识 Page 生成器。
 */
public interface KnowledgePageGenerator {

    GeneratedKnowledgePage generate(String fileName, String parsedText);
}
