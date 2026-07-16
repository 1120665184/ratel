package org.quyq.gwsu.kit.knowledge.engine;

/**
 * 知识源文档解析器。
 */
public interface KnowledgeDocumentParser {

    ParsedKnowledgeDocument parse(String fileId);
}
