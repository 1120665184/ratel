package org.quyq.gwsu.kit.api.knowledge.enums;

/**
 * 知识文档导入阶段。
 */
public enum KnowledgeIngestStage {

    PARSE,
    GENERATE_PAGE,
    MERGE_PAGE,
    BUILD_CHUNK,
    INDEX_ES
}
