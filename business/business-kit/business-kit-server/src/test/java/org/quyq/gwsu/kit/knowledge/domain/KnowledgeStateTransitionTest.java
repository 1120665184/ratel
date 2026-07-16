package org.quyq.gwsu.kit.knowledge.domain;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus.PROCESSING;
import static org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus.SUCCEEDED;

class KnowledgeStateTransitionTest {

    @Test
    void uploadedDocumentCanTransitToProcessing() {
        assertTrue(KnowledgeDocumentStatus.UPLOADED.canTransitTo(PROCESSING));
    }

    @Test
    void processedDocumentCannotTransitToProcessing() {
        assertFalse(KnowledgeDocumentStatus.PROCESSED.canTransitTo(PROCESSING));
    }

    @Test
    void runningIngestTaskCanTransitToSucceeded() {
        assertTrue(KnowledgeIngestTaskStatus.RUNNING.canTransitTo(SUCCEEDED));
    }
}
