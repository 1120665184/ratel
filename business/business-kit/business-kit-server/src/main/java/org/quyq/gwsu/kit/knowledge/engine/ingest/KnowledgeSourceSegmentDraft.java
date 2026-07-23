package org.quyq.gwsu.kit.knowledge.engine.ingest;

/**
 * 源文档结构化片段。
 */
public record KnowledgeSourceSegmentDraft(
        int segmentNo,
        String segmentType,
        String headingPath,
        String sourceLocator,
        String content) {
}
