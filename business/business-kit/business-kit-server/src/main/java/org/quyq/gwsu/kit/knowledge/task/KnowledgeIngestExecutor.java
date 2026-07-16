package org.quyq.gwsu.kit.knowledge.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestStage;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeSourceDocument;
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
        KnowledgeIngestTask task = loadTask(taskId);
        try {
            markTaskRunning(task);
            KnowledgeSourceDocument sourceDocument = loadSourceDocument(task);

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

    private KnowledgeIngestTask loadTask(String taskId) {
        KnowledgeIngestTask task = ingestTaskMapper.selectOne(new LambdaQueryWrapper<KnowledgeIngestTask>()
                .eq(KnowledgeIngestTask::getId, taskId)
                .eq(KnowledgeIngestTask::getDeleted, false));
        if (Objects.isNull(task)) {
            throw new BusinessException(KitErrorCode.E03002);
        }
        return task;
    }

    private KnowledgeSourceDocument loadSourceDocument(KnowledgeIngestTask task) {
        KnowledgeSourceDocument sourceDocument = sourceDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeSourceDocument>()
                .eq(KnowledgeSourceDocument::getId, task.getSourceDocumentId())
                .eq(KnowledgeSourceDocument::getTenantId, task.getTenantId())
                .eq(KnowledgeSourceDocument::getDeleted, false));
        if (Objects.isNull(sourceDocument)) {
            throw new BusinessException(KitErrorCode.E03001);
        }
        return sourceDocument;
    }

    private void markTaskRunning(KnowledgeIngestTask task) {
        updateTask(task.getId(), new KnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.RUNNING)
                .setStartedAt(LocalDateTime.now()));
        updateSourceStatus(task.getSourceDocumentId(), task.getTenantId(), new KnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSING));
    }

    private void updateStage(String taskId, KnowledgeIngestStage stage) {
        updateTask(taskId, new KnowledgeIngestTask().setCurrentStage(stage));
    }

    private void markSucceeded(KnowledgeIngestTask task, KnowledgeSourceDocument sourceDocument) {
        updateTask(task.getId(), new KnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.SUCCEEDED)
                .setFinishedAt(LocalDateTime.now()));
        updateSourceStatus(sourceDocument.getId(), task.getTenantId(), new KnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.PROCESSED)
                .setProcessedAt(LocalDateTime.now()));
    }

    private void markFailed(KnowledgeIngestTask task, Exception ex) {
        updateTask(task.getId(), new KnowledgeIngestTask()
                .setTaskStatus(KnowledgeIngestTaskStatus.FAILED)
                .setErrorMessage(ex.getMessage())
                .setFinishedAt(LocalDateTime.now()));
        updateSourceStatus(task.getSourceDocumentId(), task.getTenantId(), new KnowledgeSourceDocument()
                .setDocumentStatus(KnowledgeDocumentStatus.FAILED)
                .setProcessMessage(ex.getMessage()));
    }

    private void updateTask(String taskId, KnowledgeIngestTask update) {
        ingestTaskMapper.update(update, new LambdaUpdateWrapper<KnowledgeIngestTask>()
                .eq(KnowledgeIngestTask::getId, taskId)
                .eq(KnowledgeIngestTask::getDeleted, false));
    }

    private void updateSourceStatus(String sourceDocumentId, String tenantId, KnowledgeSourceDocument update) {
        sourceDocumentMapper.update(update, new LambdaUpdateWrapper<KnowledgeSourceDocument>()
                .eq(KnowledgeSourceDocument::getId, sourceDocumentId)
                .eq(KnowledgeSourceDocument::getTenantId, tenantId)
                .eq(KnowledgeSourceDocument::getDeleted, false));
    }
}
