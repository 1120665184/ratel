package org.quyq.gwsu.kit.knowledge.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestStage;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.AnalyzedKnowledgeSource;
import org.quyq.gwsu.kit.knowledge.engine.GeneratedKnowledgePage;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkBuildRequest;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkDocument;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeDocumentParser;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeIngestSanitizer;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageGenerator;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageGenerationRequest;
import org.quyq.gwsu.kit.knowledge.engine.LongSourceAnalysisService;
import org.quyq.gwsu.kit.knowledge.engine.ParsedKnowledgeDocument;
import org.quyq.gwsu.kit.knowledge.engine.SanitizedKnowledgeSource;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.KnowledgePageMergeService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 知识源文档导入执行器。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeIngestExecutor {

    private final KnowledgeIngestTaskMapper ingestTaskMapper;

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    private final KnowledgePageBlockMapper pageBlockMapper;

    private final KnowledgePageSourceRefMapper pageSourceRefMapper;

    private final KnowledgeProperties knowledgeProperties;

    private final KnowledgeDocumentParser documentParser;

    private final KnowledgeIngestSanitizer ingestSanitizer;

    private final LongSourceAnalysisService longSourceAnalysisService;

    private final KnowledgePageGenerator pageGenerator;

    private final KnowledgePageMergeService pageMergeService;

    private final KnowledgeChunkBuilder chunkBuilder;

    private final KnowledgeChunkEmbeddingService chunkEmbeddingService;

    private final KnowledgeChunkIndexRepository chunkIndexRepository;

    public void execute(String taskId) {
        KitKnowledgeIngestTask task = loadTask(taskId);
        try {
            markTaskRunning(task);
            KitKnowledgeSourceDocument sourceDocument = loadSourceDocument(task);

            updateStage(task.getId(), KnowledgeIngestStage.PARSE);
            ParsedKnowledgeDocument parsedDocument = documentParser.parse(sourceDocument.getFileId());

            updateStage(task.getId(), KnowledgeIngestStage.SANITIZE_SOURCE);
            SanitizedKnowledgeSource sanitizedSource = ingestSanitizer.sanitize(parsedDocument);

            updateStage(task.getId(), KnowledgeIngestStage.ANALYZE_SOURCE);
            AnalyzedKnowledgeSource analyzedSource = longSourceAnalysisService.analyze(task, parsedDocument, sanitizedSource);

            updateStage(task.getId(), KnowledgeIngestStage.GENERATE_PAGE);
            GeneratedKnowledgePage generatedPage = pageGenerator.generate(new KnowledgePageGenerationRequest(
                    parsedDocument.fileName(),
                    analyzedSource.sourceLanguage(),
                    analyzedSource.analysisDigest(),
                    analyzedSource.boundedSourceText(),
                    knowledgeProperties.getWikiOutputLanguage()));

            updateStage(task.getId(), KnowledgeIngestStage.MERGE_PAGE);
            String pageVersionId = pageMergeService.publish(sourceDocument, generatedPage);

            updateStage(task.getId(), KnowledgeIngestStage.BUILD_CHUNK);
            KitKnowledgePageVersion pageVersion = loadPageVersion(pageVersionId);
            List<KitKnowledgePageBlock> blocks = pageBlockMapper.selectByVersionId(pageVersionId);
            List<KitKnowledgePageSourceRef> sourceRefs = loadSourceRefs(blocks);
            List<KnowledgeChunkDocument> chunks = chunkBuilder.build(new KnowledgeChunkBuildRequest(
                    pageVersion.getPageId(),
                    generatedPage.title(),
                    pageVersion,
                    blocks,
                    sourceRefs));

            updateStage(task.getId(), KnowledgeIngestStage.EMBED_CHUNK);
            boolean embeddingCompleted = chunkEmbeddingService.embedChunks(chunks);
            updateSourceEmbeddingCompleted(sourceDocument.getId(), embeddingCompleted);

            updateStage(task.getId(), KnowledgeIngestStage.INDEX_ES);
            chunkIndexRepository.replacePageVersion(pageVersion.getPageId(), pageVersion.getId(), chunks);
            markSucceeded(task, sourceDocument);
        } catch (Exception ex) {
            markFailed(task, ex);
            throw ex;
        }
    }

    private KitKnowledgeIngestTask loadTask(String taskId) {
        KitKnowledgeIngestTask task = ingestTaskMapper.selectOne(new LambdaQueryWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(task)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        return task;
    }

    private KitKnowledgeSourceDocument loadSourceDocument(KitKnowledgeIngestTask task) {
        KitKnowledgeSourceDocument sourceDocument = sourceDocumentMapper.selectOne(new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, task.getSourceDocumentId())
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(sourceDocument)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        return sourceDocument;
    }

    private KitKnowledgePageVersion loadPageVersion(String pageVersionId) {
        KitKnowledgePageVersion pageVersion = pageVersionMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePageVersion>()
                .eq(KitKnowledgePageVersion::getId, pageVersionId)
                .eq(KitKnowledgePageVersion::getDeleted, false));
        if (Objects.isNull(pageVersion)) {
            throw new BusinessException(KitErrorCode.E03009);
        }
        return pageVersion;
    }

    private List<KitKnowledgePageSourceRef> loadSourceRefs(List<KitKnowledgePageBlock> blocks) {
        if (CollectionUtils.isEmpty(blocks)) {
            return List.of();
        }
        return pageSourceRefMapper.selectByPageBlockIds(blocks.stream()
                .map(KitKnowledgePageBlock::getId)
                .toList());
    }

    private void markTaskRunning(KitKnowledgeIngestTask task) {
        updateTask(task.getId(), new KitKnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.RUNNING)
                .setStartedAt(LocalDateTime.now()));
        updateSourceStatus(task.getSourceDocumentId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSING)
                .setEmbeddingCompleted(false));
    }

    private void updateStage(String taskId, KnowledgeIngestStage stage) {
        updateTask(taskId, new KitKnowledgeIngestTask().setCurrentStage(stage));
    }

    private void markSucceeded(KitKnowledgeIngestTask task, KitKnowledgeSourceDocument sourceDocument) {
        updateTask(task.getId(), new KitKnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.SUCCEEDED)
                .setFinishedAt(LocalDateTime.now()));
        updateSourceStatus(sourceDocument.getId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSED)
                .setProcessedAt(LocalDateTime.now()));
    }

    private void markFailed(KitKnowledgeIngestTask task, Exception ex) {
        updateTask(task.getId(), new KitKnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.FAILED)
                .setErrorMessage(ex.getMessage())
                .setFinishedAt(LocalDateTime.now()));
        updateSourceStatus(task.getSourceDocumentId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.FAILED)
                .setProcessMessage(ex.getMessage()));
    }

    private void updateTask(String taskId, KitKnowledgeIngestTask update) {
        ingestTaskMapper.update(update, new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
    }

    private void updateSourceStatus(String sourceDocumentId, KitKnowledgeSourceDocument update) {
        sourceDocumentMapper.update(update, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
    }

    private void updateSourceEmbeddingCompleted(String sourceDocumentId, boolean embeddingCompleted) {
        updateSourceStatus(sourceDocumentId, new KitKnowledgeSourceDocument()
                .setEmbeddingCompleted(embeddingCompleted));
    }
}
