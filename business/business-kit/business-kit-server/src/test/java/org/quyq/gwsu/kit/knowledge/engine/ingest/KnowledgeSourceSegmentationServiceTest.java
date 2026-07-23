package org.quyq.gwsu.kit.knowledge.engine.ingest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeSourceSegmentationServiceTest {

    @Test
    void shouldKeepSegmentLocatorAlignedWithSegmentNo() {
        KnowledgeSourceSegmentationService service = new KnowledgeSourceSegmentationService();
        ParsedKnowledgeDocument parsedDocument = ParsedKnowledgeDocument.of("demo.txt", "ignored");
        SanitizedKnowledgeSource sanitizedSource = new SanitizedKnowledgeSource("Paragraph: 第一段\nContent: 第二段", List.of());

        SegmentedKnowledgeSource result = service.segment(parsedDocument, sanitizedSource);

        assertEquals(2, result.segments().size());
        assertEquals(1, result.segments().get(0).segmentNo());
        assertEquals("document/segment:1", result.segments().get(0).sourceLocator());
        assertEquals(2, result.segments().get(1).segmentNo());
        assertEquals("document/segment:2", result.segments().get(1).sourceLocator());
    }
}
