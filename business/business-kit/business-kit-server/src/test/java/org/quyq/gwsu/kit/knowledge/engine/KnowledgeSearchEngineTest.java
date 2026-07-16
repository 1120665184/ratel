package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSearchEngineTest {

    @Test
    void searchUsesVisibleSourceDocumentsAsEsTermsFilter() {
        IKnowledgeSourceDocumentService sourceDocumentService = mock(IKnowledgeSourceDocumentService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeSearchRerankService searchRerankService = mock(KnowledgeSearchRerankService.class);
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setSearchSize(5);
        KnowledgeSearchEngine searchEngine = new KnowledgeSearchEngine(
                sourceDocumentService,
                chunkIndexRepository,
                chunkEmbeddingService,
                searchRerankService,
                properties);
        KnowledgeSearchDTO dto = new KnowledgeSearchDTO();
        dto.setTenantId("tenant-1");
        dto.setKeyword("安全");
        dto.setRoleCodes(List.of("ROLE_A"));
        Optional<float[]> queryEmbedding = Optional.of(new float[] {0.1f, 0.2f});
        List<KnowledgeSearchResultVO> recalled = List.of(new KnowledgeSearchResultVO().setSourceDocumentId("role-a-document"));
        when(sourceDocumentService.listVisibleSourceDocumentIds("tenant-1", dto.getRoleCodes()))
                .thenReturn(List.of("open-document", "role-a-document"));
        when(chunkEmbeddingService.embedQuery("安全")).thenReturn(queryEmbedding);
        when(chunkIndexRepository.search("安全", List.of("open-document", "role-a-document"), 15, queryEmbedding))
                .thenReturn(recalled);
        when(searchRerankService.rerank("安全", recalled, 5)).thenReturn(recalled);

        List<KnowledgeSearchResultVO> results = searchEngine.search(dto);

        assertEquals(1, results.size());
        verify(sourceDocumentService).listVisibleSourceDocumentIds("tenant-1", dto.getRoleCodes());
        verify(chunkEmbeddingService).embedQuery("安全");
        verify(chunkIndexRepository).search("安全", List.of("open-document", "role-a-document"), 15, queryEmbedding);
        verify(searchRerankService).rerank("安全", recalled, 5);
    }

    @Test
    void findAdjacentChunkUsesVisibleSourceDocumentsAsEsFilter() {
        IKnowledgeSourceDocumentService sourceDocumentService = mock(IKnowledgeSourceDocumentService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeSearchRerankService searchRerankService = mock(KnowledgeSearchRerankService.class);
        KnowledgeProperties properties = new KnowledgeProperties();
        KnowledgeSearchEngine searchEngine = new KnowledgeSearchEngine(
                sourceDocumentService,
                chunkIndexRepository,
                chunkEmbeddingService,
                searchRerankService,
                properties);
        when(sourceDocumentService.listVisibleSourceDocumentIds("tenant-1", List.of("ROLE_A")))
                .thenReturn(List.of("open-document", "role-a-document"));
        when(chunkIndexRepository.findAdjacentChunk(
                "chunk-1",
                KnowledgeChunkDirection.NEXT,
                List.of("open-document", "role-a-document")))
                .thenReturn(Optional.of(new KnowledgeSearchResultVO().setChunkId("chunk-2")));

        Optional<KnowledgeSearchResultVO> result = searchEngine.findAdjacentChunk(
                "tenant-1",
                List.of("ROLE_A"),
                "chunk-1",
                KnowledgeChunkDirection.NEXT);

        assertEquals("chunk-2", result.orElseThrow().getChunkId());
        verify(sourceDocumentService).listVisibleSourceDocumentIds("tenant-1", List.of("ROLE_A"));
        verify(chunkIndexRepository).findAdjacentChunk(
                "chunk-1",
                KnowledgeChunkDirection.NEXT,
                List.of("open-document", "role-a-document"));
    }
}
