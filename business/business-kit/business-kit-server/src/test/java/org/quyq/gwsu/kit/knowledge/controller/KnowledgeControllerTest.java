package org.quyq.gwsu.kit.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeChunkAdjacentDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeIngestTaskQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgePageQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgePageSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageDetailVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageVO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeSearchEngine;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeIngestTaskService;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgePageCommandService;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgePageQueryService;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;
import org.quyq.gwsu.kit.knowledge.service.KnowledgeIngestApplicationService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeControllerTest {

    @Test
    void searchDelegatesToSearchEngine() {
        KnowledgeIngestApplicationService applicationService = mock(KnowledgeIngestApplicationService.class);
        IKnowledgeSourceDocumentService sourceDocumentService = mock(IKnowledgeSourceDocumentService.class);
        IKnowledgeIngestTaskService ingestTaskService = mock(IKnowledgeIngestTaskService.class);
        IKnowledgePageCommandService pageCommandService = mock(IKnowledgePageCommandService.class);
        IKnowledgePageQueryService pageQueryService = mock(IKnowledgePageQueryService.class);
        KnowledgeSearchEngine searchEngine = mock(KnowledgeSearchEngine.class);
        KnowledgeController controller = new KnowledgeController(
                applicationService,
                sourceDocumentService,
                ingestTaskService,
                pageCommandService,
                pageQueryService,
                searchEngine);
        KnowledgeSearchDTO dto = new KnowledgeSearchDTO();
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
        KnowledgeIngestApplicationService applicationService = mock(KnowledgeIngestApplicationService.class);
        IKnowledgeSourceDocumentService sourceDocumentService = mock(IKnowledgeSourceDocumentService.class);
        IKnowledgeIngestTaskService ingestTaskService = mock(IKnowledgeIngestTaskService.class);
        IKnowledgePageCommandService pageCommandService = mock(IKnowledgePageCommandService.class);
        IKnowledgePageQueryService pageQueryService = mock(IKnowledgePageQueryService.class);
        KnowledgeSearchEngine searchEngine = mock(KnowledgeSearchEngine.class);
        KnowledgeController controller = new KnowledgeController(
                applicationService,
                sourceDocumentService,
                ingestTaskService,
                pageCommandService,
                pageQueryService,
                searchEngine);
        KnowledgeChunkAdjacentDTO dto = new KnowledgeChunkAdjacentDTO();
        dto.setRoleCodes(List.of("ROLE_A"));
        dto.setChunkId("chunk-1");
        dto.setDirection(KnowledgeChunkDirection.NEXT);
        KnowledgeSearchResultVO adjacent = new KnowledgeSearchResultVO().setChunkId("chunk-2");
        when(searchEngine.findAdjacentChunk(List.of("ROLE_A"), "chunk-1", KnowledgeChunkDirection.NEXT))
                .thenReturn(Optional.of(adjacent));

        R<KnowledgeSearchResultVO> result = controller.findAdjacentChunk(dto);

        assertEquals(adjacent, result.data());
        verify(searchEngine).findAdjacentChunk(List.of("ROLE_A"), "chunk-1", KnowledgeChunkDirection.NEXT);
    }

    @Test
    void managementEndpointsDelegateToServices() {
        KnowledgeIngestApplicationService applicationService = mock(KnowledgeIngestApplicationService.class);
        IKnowledgeSourceDocumentService sourceDocumentService = mock(IKnowledgeSourceDocumentService.class);
        IKnowledgeIngestTaskService ingestTaskService = mock(IKnowledgeIngestTaskService.class);
        IKnowledgePageCommandService pageCommandService = mock(IKnowledgePageCommandService.class);
        IKnowledgePageQueryService pageQueryService = mock(IKnowledgePageQueryService.class);
        KnowledgeSearchEngine searchEngine = mock(KnowledgeSearchEngine.class);
        KnowledgeController controller = new KnowledgeController(
                applicationService,
                sourceDocumentService,
                ingestTaskService,
                pageCommandService,
                pageQueryService,
                searchEngine);
        KnowledgeDocumentSaveDTO saveDTO = new KnowledgeDocumentSaveDTO();
        KnowledgePageSaveDTO pageSaveDTO = new KnowledgePageSaveDTO();
        KnowledgeDocumentQueryDTO documentQueryDTO = new KnowledgeDocumentQueryDTO();
        KnowledgeIngestTaskQueryDTO taskQueryDTO = new KnowledgeIngestTaskQueryDTO();
        KnowledgePageQueryDTO pageQueryDTO = new KnowledgePageQueryDTO();
        @SuppressWarnings("unchecked")
        IPage<KnowledgeDocumentVO> documentPage = mock(IPage.class);
        @SuppressWarnings("unchecked")
        IPage<KnowledgePageVO> pagePage = mock(IPage.class);
        KnowledgeDocumentVO documentVO = new KnowledgeDocumentVO().setId("document-1");
        KnowledgePageDetailVO pageDetailVO = new KnowledgePageDetailVO();
        pageDetailVO.setId("page-1");
        when(applicationService.saveDocumentAndSubmit(saveDTO)).thenReturn("task-1");
        when(pageCommandService.savePage(pageSaveDTO)).thenReturn("page-1");
        when(sourceDocumentService.pageDocuments(documentQueryDTO)).thenReturn(documentPage);
        when(sourceDocumentService.getDocument("document-1")).thenReturn(documentVO);
        when(pageQueryService.pagePages(pageQueryDTO)).thenReturn(pagePage);
        when(pageQueryService.getPage("page-1")).thenReturn(pageDetailVO);
        when(applicationService.retryAndSubmit("task-1")).thenReturn("task-2");

        assertEquals("task-1", controller.saveDocument(saveDTO).data());
        assertEquals("page-1", controller.savePage(pageSaveDTO).data());
        assertEquals(documentPage, controller.pageDocuments(documentQueryDTO).data());
        assertEquals(documentVO, controller.getDocument("document-1", documentQueryDTO).data());
        assertEquals(pagePage, controller.pagePages(pageQueryDTO).data());
        assertEquals(pageDetailVO, controller.getPage("page-1", pageQueryDTO).data());
        assertEquals("task-2", controller.retryTask("task-1", taskQueryDTO).data());
    }
}
