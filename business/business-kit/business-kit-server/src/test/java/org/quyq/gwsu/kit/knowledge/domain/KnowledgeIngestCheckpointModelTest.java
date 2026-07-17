package org.quyq.gwsu.kit.knowledge.domain;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestStage;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestAnalysisCheckpointStatus;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeIngestCheckpointModelTest {

    @Test
    void ingestStagesContainSourceAnalysisPipeline() {
        List<KnowledgeIngestStage> stages = List.of(KnowledgeIngestStage.values());

        assertTrue(stages.contains(KnowledgeIngestStage.SANITIZE_SOURCE));
        assertTrue(stages.contains(KnowledgeIngestStage.ANALYZE_SOURCE));
    }

    @Test
    void knowledgePropertiesProvideProductionDefaults() {
        KnowledgeProperties properties = new KnowledgeProperties();

        assertEquals("zh-CN", properties.getWikiOutputLanguage());
        assertEquals(6000, properties.getAnalysisChunkTokenCount());
        assertEquals(500, properties.getAnalysisChunkOverlapTokenCount());
        assertEquals(12000, properties.getGenerationContextTokenCount());
        assertEquals(30, properties.getHybridRecallSize());
        assertEquals(1, properties.getAnswerContextAdjacentChunkCount());
    }

    @Test
    void checkpointDomainExposesRecoveryFields() {
        KitKnowledgeIngestAnalysisCheckpoint checkpoint = new KitKnowledgeIngestAnalysisCheckpoint()
                .setId("checkpoint-1")
                .setIngestTaskId("task-1")
                .setChunkNo(2)
                .setChunkContentHash("sha256")
                .setAnalysisDigest("摘要")
                .setCheckpointStatus(KnowledgeIngestAnalysisCheckpointStatus.SUCCEEDED)
                .setSourceLanguage("fr");

        assertEquals("checkpoint-1", checkpoint.getId());
        assertEquals("task-1", checkpoint.getIngestTaskId());
        assertEquals(2, checkpoint.getChunkNo());
        assertEquals("sha256", checkpoint.getChunkContentHash());
        assertEquals("摘要", checkpoint.getAnalysisDigest());
        assertEquals(KnowledgeIngestAnalysisCheckpointStatus.SUCCEEDED, checkpoint.getCheckpointStatus());
        assertEquals("fr", checkpoint.getSourceLanguage());
    }

    @Test
    void checkpointStatusFollowsIndependentLifecycle() {
        assertTrue(KnowledgeIngestAnalysisCheckpointStatus.PENDING
                .canTransitTo(KnowledgeIngestAnalysisCheckpointStatus.RUNNING));
        assertTrue(KnowledgeIngestAnalysisCheckpointStatus.RUNNING
                .canTransitTo(KnowledgeIngestAnalysisCheckpointStatus.SUCCEEDED));
        assertFalse(KnowledgeIngestAnalysisCheckpointStatus.SUCCEEDED
                .canTransitTo(KnowledgeIngestAnalysisCheckpointStatus.RUNNING));
    }

    @Test
    void propertiesRejectInvalidPositiveAndOverlapBoundaries() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        KnowledgeProperties nonPositive = new KnowledgeProperties();
        nonPositive.setHybridRecallSize(0);
        assertFalse(validator.validate(nonPositive).isEmpty());

        KnowledgeProperties sameSizeOverlap = new KnowledgeProperties();
        sameSizeOverlap.setAnalysisChunkTokenCount(6000);
        sameSizeOverlap.setAnalysisChunkOverlapTokenCount(6000);
        assertFalse(validator.validate(sameSizeOverlap).isEmpty());

        KnowledgeProperties blankWikiOutputLanguage = new KnowledgeProperties();
        blankWikiOutputLanguage.setWikiOutputLanguage("  ");
        assertFalse(validator.validate(blankWikiOutputLanguage).isEmpty());

        KnowledgeProperties valid = new KnowledgeProperties();
        valid.setAnalysisChunkTokenCount(6000);
        valid.setAnalysisChunkOverlapTokenCount(5999);
        assertTrue(validator.validate(valid).isEmpty());
    }
}
