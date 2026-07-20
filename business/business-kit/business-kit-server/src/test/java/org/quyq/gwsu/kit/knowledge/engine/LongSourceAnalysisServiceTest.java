package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestAnalysisCheckpointStatus;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestAnalysisCheckpoint;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestAnalysisCheckpointMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LongSourceAnalysisServiceTest {

    @Test
    void shouldResumeFromExistingChunkDigestAndOnlyAnalyzeChangedChunks() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setAnalysisChunkTokenCount(15);
        properties.setAnalysisChunkOverlapTokenCount(2);
        properties.setGenerationContextTokenCount(120);
        KnowledgeContextBudgetService budgetService = new KnowledgeContextBudgetService(properties);
        KnowledgeAnalysisPromptBuilder promptBuilder = new KnowledgeAnalysisPromptBuilder();
        KnowledgeAnalysisModelClient modelClient = mock(KnowledgeAnalysisModelClient.class);
        KnowledgeIngestAnalysisCheckpointMapper mapper = mock(KnowledgeIngestAnalysisCheckpointMapper.class);
        LongSourceAnalysisService service = new LongSourceAnalysisService(
                properties,
                budgetService,
                promptBuilder,
                modelClient,
                mapper);

        String sourceText = "# 第一节\nalphaalphaalpha\n\n# 第二节\nbetabetabeta\n\n# 第三节\ngammagammagamma";
        List<String> chunks = service.split(sourceText, budgetService.resolveBudget());
        KitKnowledgeIngestTask task = new KitKnowledgeIngestTask().setId("task-1");
        ParsedKnowledgeDocument parsed = new ParsedKnowledgeDocument("guide.md", "text/markdown", "fr", sourceText, List.of());
        SanitizedKnowledgeSource sanitized = new SanitizedKnowledgeSource(sourceText, List.of());

        when(mapper.selectByTaskId("task-1")).thenReturn(List.of(
                checkpoint("task-1", 1, chunks.get(0), "第一节摘要"),
                checkpoint("task-1", 2, "stale", "旧第二节摘要")));
        when(modelClient.analyzeChunk(any())).thenReturn("第二节新摘要", "第三节摘要");
        when(modelClient.summarizeDigests(any())).thenReturn("第一节摘要\n第二节新摘要\n第三节摘要");

        AnalyzedKnowledgeSource analyzed = service.analyze(task, parsed, sanitized);

        assertEquals("fr", analyzed.sourceLanguage());
        assertTrue(analyzed.analysisDigest().contains("第一节摘要"));
        assertFalse(analyzed.analysisDigest().isBlank());
        verify(modelClient, org.mockito.Mockito.times(2)).analyzeChunk(any());
        verify(modelClient).summarizeDigests(any());
        verify(mapper).insert(any(KitKnowledgeIngestAnalysisCheckpoint.class));
        verify(mapper).updateById(any(KitKnowledgeIngestAnalysisCheckpoint.class));
    }

    @Test
    void shouldNotCallSummaryModelWhenOnlyOneChunkExists() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setAnalysisChunkTokenCount(200);
        properties.setAnalysisChunkOverlapTokenCount(10);
        KnowledgeContextBudgetService budgetService = new KnowledgeContextBudgetService(properties);
        KnowledgeAnalysisModelClient modelClient = mock(KnowledgeAnalysisModelClient.class);
        KnowledgeIngestAnalysisCheckpointMapper mapper = mock(KnowledgeIngestAnalysisCheckpointMapper.class);
        LongSourceAnalysisService service = new LongSourceAnalysisService(
                properties,
                budgetService,
                new KnowledgeAnalysisPromptBuilder(),
                modelClient,
                mapper);
        KitKnowledgeIngestTask task = new KitKnowledgeIngestTask().setId("task-1");
        String sourceText = "# 单节\n简短内容";
        when(mapper.selectByTaskId("task-1")).thenReturn(List.of());
        when(modelClient.analyzeChunk(any())).thenReturn("单节摘要");

        AnalyzedKnowledgeSource analyzed = service.analyze(
                task,
                new ParsedKnowledgeDocument("guide.md", "text/markdown", "zh", sourceText, List.of()),
                new SanitizedKnowledgeSource(sourceText, List.of()));

        assertEquals("单节摘要", analyzed.analysisDigest());
        verify(modelClient, never()).summarizeDigests(any());
    }

    @Test
    void shouldReturnEmptyDigestWhenSanitizedSourceHasNoUsableText() {
        KnowledgeProperties properties = new KnowledgeProperties();
        KnowledgeContextBudgetService budgetService = new KnowledgeContextBudgetService(properties);
        KnowledgeAnalysisModelClient modelClient = mock(KnowledgeAnalysisModelClient.class);
        KnowledgeIngestAnalysisCheckpointMapper mapper = mock(KnowledgeIngestAnalysisCheckpointMapper.class);
        LongSourceAnalysisService service = new LongSourceAnalysisService(
                properties,
                budgetService,
                new KnowledgeAnalysisPromptBuilder(),
                modelClient,
                mapper);
        KitKnowledgeIngestTask task = new KitKnowledgeIngestTask().setId("task-1");

        AnalyzedKnowledgeSource analyzed = service.analyze(
                task,
                new ParsedKnowledgeDocument("empty.md", "text/markdown", "en", "", List.of()),
                new SanitizedKnowledgeSource("", List.of()));

        assertEquals("en", analyzed.sourceLanguage());
        assertEquals("", analyzed.analysisDigest());
        assertEquals("", analyzed.boundedSourceText());
        verify(mapper, never()).selectByTaskId(any());
        verify(modelClient, never()).analyzeChunk(any());
        verify(modelClient, never()).summarizeDigests(any());
    }

    private KitKnowledgeIngestAnalysisCheckpoint checkpoint(String taskId, int chunkNo, String chunkText, String digest) {
        return new KitKnowledgeIngestAnalysisCheckpoint()
                .setId("checkpoint-" + chunkNo)
                .setIngestTaskId(taskId)
                .setChunkNo(chunkNo)
                .setChunkContentHash(org.springframework.util.DigestUtils.md5DigestAsHex(chunkText.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .setAnalysisDigest(digest)
                .setCheckpointStatus(KnowledgeIngestAnalysisCheckpointStatus.SUCCEEDED)
                .setSourceLanguage("fr");
    }
}
