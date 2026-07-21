package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeIngestApplicationServiceTest {

    @Test
    void saveDocumentAndSubmitShouldCreateTaskAndDispatch() {
        IKnowledgeSourceDocumentService sourceDocumentService = mock(IKnowledgeSourceDocumentService.class);
        IKnowledgeIngestTaskService ingestTaskService = mock(IKnowledgeIngestTaskService.class);
        KnowledgeIngestDispatcher dispatcher = mock(KnowledgeIngestDispatcher.class);
        KnowledgeIngestApplicationService service = new KnowledgeIngestApplicationService(
                sourceDocumentService,
                ingestTaskService,
                dispatcher);
        KnowledgeDocumentSaveDTO dto = new KnowledgeDocumentSaveDTO();
        when(sourceDocumentService.saveDocument(dto)).thenReturn("document-1");
        when(ingestTaskService.createOrResetTask("document-1", false)).thenReturn("task-1");

        String taskId = service.saveDocumentAndSubmit(dto);

        assertEquals("task-1", taskId);
        verify(sourceDocumentService).saveDocument(dto);
        verify(ingestTaskService).createOrResetTask("document-1", false);
        verify(dispatcher).dispatchAfterCommit("task-1");
    }

    @Test
    void retryAndSubmitShouldDispatchRetryTask() {
        IKnowledgeSourceDocumentService sourceDocumentService = mock(IKnowledgeSourceDocumentService.class);
        IKnowledgeIngestTaskService ingestTaskService = mock(IKnowledgeIngestTaskService.class);
        KnowledgeIngestDispatcher dispatcher = mock(KnowledgeIngestDispatcher.class);
        KnowledgeIngestApplicationService service = new KnowledgeIngestApplicationService(
                sourceDocumentService,
                ingestTaskService,
                dispatcher);
        when(ingestTaskService.retry("task-old")).thenReturn("task-old");

        String taskId = service.retryAndSubmit("task-old");

        assertEquals("task-old", taskId);
        verify(ingestTaskService).retry("task-old");
        verify(dispatcher).dispatchAfterCommit("task-old");
    }
}
