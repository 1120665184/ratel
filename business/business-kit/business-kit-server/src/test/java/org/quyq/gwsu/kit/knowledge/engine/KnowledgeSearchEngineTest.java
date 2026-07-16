package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSearchEngineTest {

    @Test
    void searchUsesVisibleSourceDocumentsAsEsTermsFilter() {
        IKnowledgeSourceDocumentService sourceDocumentService = mock(IKnowledgeSourceDocumentService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setSearchSize(5);
        KnowledgeSearchEngine searchEngine = new KnowledgeSearchEngine(sourceDocumentService, chunkIndexRepository, properties);
        KnowledgeSearchDTO dto = new KnowledgeSearchDTO();
        dto.setTenantId("tenant-1");
        dto.setKeyword("安全");
        dto.setRoleCodes(List.of("ROLE_A"));
        when(sourceDocumentService.listVisibleSourceDocumentIds("tenant-1", dto.getRoleCodes()))
                .thenReturn(List.of("open-document", "role-a-document"));
        when(chunkIndexRepository.search("安全", List.of("open-document", "role-a-document"), 5))
                .thenReturn(List.of(new KnowledgeSearchResultVO().setSourceDocumentId("role-a-document")));

        List<KnowledgeSearchResultVO> results = searchEngine.search(dto);

        assertEquals(1, results.size());
        verify(sourceDocumentService).listVisibleSourceDocumentIds("tenant-1", dto.getRoleCodes());
        verify(chunkIndexRepository).search("安全", List.of("open-document", "role-a-document"), 5);
    }
}
