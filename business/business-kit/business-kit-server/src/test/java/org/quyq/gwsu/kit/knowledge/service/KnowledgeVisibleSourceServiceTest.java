package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentRoleMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentServiceImpl;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeVisibleSourceServiceTest {

    private static final String TENANT_ID = "tenant-a";

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper = mock(KnowledgeSourceDocumentMapper.class);
    private final KnowledgeSourceDocumentRoleMapper roleMapper = mock(KnowledgeSourceDocumentRoleMapper.class);
    private final KnowledgeSourceDocumentServiceImpl service =
            new KnowledgeSourceDocumentServiceImpl(sourceDocumentMapper, roleMapper);

    @Test
    void roleASeesOpenAndRoleADocument() {
        Set<String> roleCodes = Set.of("ROLE_A");
        when(sourceDocumentMapper.listVisibleSourceDocumentIds(TENANT_ID, roleCodes))
                .thenReturn(List.of("open-document", "role-a-document"));

        List<String> visibleIds = service.listVisibleSourceDocumentIds(TENANT_ID, roleCodes);

        assertEquals(List.of("open-document", "role-a-document"), visibleIds);
        verify(sourceDocumentMapper).listVisibleSourceDocumentIds(TENANT_ID, roleCodes);
    }

    @Test
    void roleCOnlySeesOpenDocument() {
        Set<String> roleCodes = Set.of("ROLE_C");
        when(sourceDocumentMapper.listVisibleSourceDocumentIds(TENANT_ID, roleCodes))
                .thenReturn(List.of("open-document"));

        List<String> visibleIds = service.listVisibleSourceDocumentIds(TENANT_ID, roleCodes);

        assertEquals(List.of("open-document"), visibleIds);
        verify(sourceDocumentMapper).listVisibleSourceDocumentIds(TENANT_ID, roleCodes);
    }
}
