package org.quyq.gwsu.kit.knowledge.service;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 知识导入应用编排服务。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeIngestApplicationService {

    private final IKnowledgeSourceDocumentService sourceDocumentService;

    private final IKnowledgeIngestTaskService ingestTaskService;

    private final KnowledgeIngestDispatcher ingestDispatcher;

    @Transactional(rollbackFor = Exception.class)
    public String saveDocumentAndSubmit(KnowledgeDocumentSaveDTO dto) {
        if (StringUtils.hasText(dto.getId())) {
            ingestTaskService.ensureNoActiveTask(dto.getId());
        }
        String sourceDocumentId = sourceDocumentService.saveDocument(dto);
        String taskId = ingestTaskService.createOrResetTask(sourceDocumentId, false);
        ingestDispatcher.dispatchAfterCommit(taskId);
        return taskId;
    }

    @Transactional(rollbackFor = Exception.class)
    public String retryAndSubmit(String taskId) {
        String retryTaskId = ingestTaskService.retry(taskId);
        ingestDispatcher.dispatchAfterCommit(retryTaskId);
        return retryTaskId;
    }
}
