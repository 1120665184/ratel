package org.quyq.gwsu.kit.knowledge.controller;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeChunkAdjacentDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeSearchEngine;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeControllerTest {

    @Test
    void searchDelegatesToSearchEngine() {
        KnowledgeSearchEngine searchEngine = mock(KnowledgeSearchEngine.class);
        KnowledgeController controller = new KnowledgeController(searchEngine);
        KnowledgeSearchDTO dto = new KnowledgeSearchDTO();
        dto.setTenantId("tenant-1");
        dto.setKeyword("安全");
        dto.setRoleCodes(List.of("ROLE_A"));
        List<KnowledgeSearchResultVO> engineResults = List.of(new KnowledgeSearchResultVO().setChunkId("chunk-1"));
        when(searchEngine.search(dto)).thenReturn(engineResults);

        R<List<KnowledgeSearchResultVO>> result = controller.search(dto);

        assertEquals(engineResults, result.data());
        verify(searchEngine).search(dto);
    }

    @Test
    void findAdjacentChunkDelegatesToSearchEngine() {
        KnowledgeSearchEngine searchEngine = mock(KnowledgeSearchEngine.class);
        KnowledgeController controller = new KnowledgeController(searchEngine);
        KnowledgeChunkAdjacentDTO dto = new KnowledgeChunkAdjacentDTO();
        dto.setTenantId("tenant-1");
        dto.setRoleCodes(List.of("ROLE_A"));
        dto.setChunkId("chunk-1");
        dto.setDirection(KnowledgeChunkDirection.NEXT);
        KnowledgeSearchResultVO adjacent = new KnowledgeSearchResultVO().setChunkId("chunk-2");
        when(searchEngine.findAdjacentChunk("tenant-1", List.of("ROLE_A"), "chunk-1", KnowledgeChunkDirection.NEXT))
                .thenReturn(Optional.of(adjacent));

        R<KnowledgeSearchResultVO> result = controller.findAdjacentChunk(dto);

        assertEquals(adjacent, result.data());
        verify(searchEngine).findAdjacentChunk("tenant-1", List.of("ROLE_A"), "chunk-1", KnowledgeChunkDirection.NEXT);
    }
}
