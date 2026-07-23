package org.quyq.gwsu.kit.knowledge.engine.ingest;

import java.util.List;

/**
 * 已分片的知识源。
 */
public record SegmentedKnowledgeSource(
        String sourceLanguage,
        List<KnowledgeSourceSegmentDraft> segments) {

    public SegmentedKnowledgeSource {
        sourceLanguage = sourceLanguage == null ? "und" : sourceLanguage;
        segments = segments == null ? List.of() : List.copyOf(segments);
    }
}
