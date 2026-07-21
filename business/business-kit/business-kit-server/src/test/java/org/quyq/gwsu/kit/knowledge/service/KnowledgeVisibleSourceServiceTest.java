package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentRoleMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentPageSyncService;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentServiceImpl;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeVisibleSourceServiceTest {

    private static final String TENANT_ID = "tenant-a";

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper = mock(KnowledgeSourceDocumentMapper.class);
    private final KnowledgeSourceDocumentRoleMapper roleMapper = mock(KnowledgeSourceDocumentRoleMapper.class);
    private final KnowledgeIngestTaskMapper ingestTaskMapper = mock(KnowledgeIngestTaskMapper.class);
    private final KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
    private final KnowledgeSourceDocumentPageSyncService pageSyncService = mock(KnowledgeSourceDocumentPageSyncService.class);
    private final KnowledgeSourceDocumentServiceImpl service =
            new KnowledgeSourceDocumentServiceImpl(
                    sourceDocumentMapper,
                    roleMapper,
                    ingestTaskMapper,
                    chunkIndexRepository,
                    pageSyncService);

    @Test
    void roleASeesOpenAndRoleADocument() {
        Set<String> roleCodes = Set.of("ROLE_A");
        when(sourceDocumentMapper.listVisibleSourceDocumentIds(roleCodes))
                .thenReturn(List.of("open-document", "role-a-document"));

        List<String> visibleIds = service.listVisibleSourceDocumentIds(roleCodes);

        assertEquals(List.of("open-document", "role-a-document"), visibleIds);
        verify(sourceDocumentMapper).listVisibleSourceDocumentIds(roleCodes);
    }

    @Test
    void roleCOnlySeesOpenDocument() {
        Set<String> roleCodes = Set.of("ROLE_C");
        when(sourceDocumentMapper.listVisibleSourceDocumentIds(roleCodes))
                .thenReturn(List.of("open-document"));

        List<String> visibleIds = service.listVisibleSourceDocumentIds(roleCodes);

        assertEquals(List.of("open-document"), visibleIds);
        verify(sourceDocumentMapper).listVisibleSourceDocumentIds(roleCodes);
    }

    @Test
    void enableDocumentReindexesCurrentWikiPages() {
        when(sourceDocumentMapper.update(isNull(), any())).thenReturn(1);

        service.updateEnabled("document-1", true);

        verify(pageSyncService).reindexCurrentPagesBySourceDocumentId("document-1");
        verify(chunkIndexRepository, never()).deleteBySourceDocumentId("document-1");
    }

    @Test
    void disableDocumentDeletesEsChunksOnly() {
        when(sourceDocumentMapper.update(isNull(), any())).thenReturn(1);

        service.updateEnabled("document-1", false);

        verify(chunkIndexRepository).deleteBySourceDocumentId("document-1");
        verify(pageSyncService, never()).reindexCurrentPagesBySourceDocumentId("document-1");
    }

    @Test
    void deleteDocumentAlsoPrunesCurrentWikiPages() {
        when(sourceDocumentMapper.selectOne(any())).thenReturn(new KitKnowledgeSourceDocument().setId("document-1"));
        when(sourceDocumentMapper.update(isNull(), any())).thenReturn(1);

        service.deleteDocument("document-1");

        verify(pageSyncService).removeSourceDocumentFromCurrentPages("document-1");
        verify(chunkIndexRepository).deleteBySourceDocumentId("document-1");
    }
}
