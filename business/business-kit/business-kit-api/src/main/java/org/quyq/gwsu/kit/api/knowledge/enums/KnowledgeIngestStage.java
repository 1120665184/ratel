package org.quyq.gwsu.kit.api.knowledge.enums;

/**
 * 知识文档导入阶段。
 */
public enum KnowledgeIngestStage {

    PARSE,
    SANITIZE_SOURCE,
    ANALYZE_SOURCE,
    GENERATE_PAGE,
    MERGE_PAGE,
    BUILD_CHUNK,
    EMBED_CHUNK,
    INDEX_ES
}
