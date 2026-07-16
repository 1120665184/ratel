package org.quyq.gwsu.kit.knowledge.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestStage;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.GeneratedKnowledgePage;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeDocumentParser;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageGenerator;
import org.quyq.gwsu.kit.knowledge.engine.ParsedKnowledgeDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.KnowledgePageMergeService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 知识源文档导入执行器。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeIngestExecutor {

    private final KnowledgeIngestTaskMapper ingestTaskMapper;

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgeDocumentParser documentParser;

    private final KnowledgePageGenerator pageGenerator;

    private final KnowledgePageMergeService pageMergeService;

    public void execute(String taskId) {
        KitKnowledgeIngestTask task = loadTask(taskId);
        try {
            markTaskRunning(task);
            KitKnowledgeSourceDocument sourceDocument = loadSourceDocument(task);

            updateStage(task.getId(), KnowledgeIngestStage.PARSE);
            ParsedKnowledgeDocument parsedDocument = documentParser.parse(sourceDocument.getFileId());

            updateStage(task.getId(), KnowledgeIngestStage.GENERATE_PAGE);
            GeneratedKnowledgePage generatedPage = pageGenerator.generate(
                    parsedDocument.fileName(),
                    parsedDocument.text());

            updateStage(task.getId(), KnowledgeIngestStage.MERGE_PAGE);
            pageMergeService.publish(task.getTenantId(), sourceDocument, generatedPage);

            updateStage(task.getId(), KnowledgeIngestStage.BUILD_CHUNK);
            updateStage(task.getId(), KnowledgeIngestStage.INDEX_ES);
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
                .eq(KitKnowledgeSourceDocument::getTenantId, task.getTenantId())
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(sourceDocument)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        return sourceDocument;
    }

    private void markTaskRunning(KitKnowledgeIngestTask task) {
        updateTask(task.getId(), new KitKnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.RUNNING)
                .setStartedAt(LocalDateTime.now()));
        updateSourceStatus(task.getSourceDocumentId(), task.getTenantId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSING));
    }

    private void updateStage(String taskId, KnowledgeIngestStage stage) {
        updateTask(taskId, new KitKnowledgeIngestTask().setCurrentStage(stage));
    }

    private void markSucceeded(KitKnowledgeIngestTask task, KitKnowledgeSourceDocument sourceDocument) {
        updateTask(task.getId(), new KitKnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.SUCCEEDED)
                .setFinishedAt(LocalDateTime.now()));
        updateSourceStatus(sourceDocument.getId(), task.getTenantId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSED)
                .setProcessedAt(LocalDateTime.now()));
    }

    private void markFailed(KitKnowledgeIngestTask task, Exception ex) {
        updateTask(task.getId(), new KitKnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.FAILED)
                .setErrorMessage(ex.getMessage())
                .setFinishedAt(LocalDateTime.now()));
        updateSourceStatus(task.getSourceDocumentId(), task.getTenantId(), new KitKnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.FAILED)
                .setProcessMessage(ex.getMessage()));
    }

    private void updateTask(String taskId, KitKnowledgeIngestTask update) {
        ingestTaskMapper.update(update, new LambdaUpdateWrapper<KitKnowledgeIngestTask>()
                .eq(KitKnowledgeIngestTask::getId, taskId)
                .eq(KitKnowledgeIngestTask::getDeleted, false));
    }

    private void updateSourceStatus(String sourceDocumentId, String tenantId, KitKnowledgeSourceDocument update) {
        sourceDocumentMapper.update(update, new LambdaUpdateWrapper<KitKnowledgeSourceDocument>()
                .eq(KitKnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KitKnowledgeSourceDocument::getTenantId, tenantId)
                .eq(KitKnowledgeSourceDocument::getDeleted, false));
    }
}
