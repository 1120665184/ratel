package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkDocument;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgeSourceDocumentPageSyncService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSourceDocumentPageSyncServiceTest {

    @Test
    void reindexCurrentPagesBySourceDocumentIdRebuildsChunksForCurrentVersion() {
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgeChunkBuilder chunkBuilder = mock(KnowledgeChunkBuilder.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        KnowledgeSourceDocumentPageSyncService service = new KnowledgeSourceDocumentPageSyncService(
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper,
                chunkBuilder,
                chunkEmbeddingService,
                chunkIndexRepository);

        KitKnowledgePage page = new KitKnowledgePage()
                .setId("page-1")
                .setTitle("应急预案")
                .setCurrentVersionId("version-1");
        KitKnowledgePageVersion version = new KitKnowledgePageVersion()
                .setId("version-1")
                .setPageId("page-1")
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED);
        KitKnowledgePageBlock block = new KitKnowledgePageBlock()
                .setId("block-1")
                .setPageVersionId("version-1")
                .setOrderNo(1)
                .setBlockType(KnowledgeBlockType.PARAGRAPH)
                .setContent("内容");
        KitKnowledgePageSourceRef ref = new KitKnowledgePageSourceRef()
                .setId("ref-1")
                .setPageBlockId("block-1")
                .setSourceDocumentId("document-1");
        List<KnowledgeChunkDocument> chunks = List.of(new KnowledgeChunkDocument().setChunkId("chunk-1"));
        when(pageMapper.selectCurrentPagesBySourceDocumentId("document-1")).thenReturn(List.of(page));
        when(pageVersionMapper.selectById("version-1")).thenReturn(version);
        when(pageBlockMapper.selectByVersionId("version-1")).thenReturn(List.of(block));
        when(pageSourceRefMapper.selectByPageBlockIds(List.of("block-1"))).thenReturn(List.of(ref));
        when(chunkBuilder.build(any())).thenReturn(chunks);

        service.reindexCurrentPagesBySourceDocumentId("document-1");

        verify(chunkEmbeddingService).embedChunks(chunks);
        verify(chunkIndexRepository).replacePageVersion("page-1", "version-1", chunks);
    }

    @Test
    void removeSourceDocumentFromCurrentPagesDeletesPageWhenNoContentRemains() {
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgeChunkBuilder chunkBuilder = mock(KnowledgeChunkBuilder.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        KnowledgeSourceDocumentPageSyncService service = new KnowledgeSourceDocumentPageSyncService(
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper,
                chunkBuilder,
                chunkEmbeddingService,
                chunkIndexRepository);

        KitKnowledgePage page = new KitKnowledgePage()
                .setId("page-1")
                .setTitle("应急预案")
                .setCurrentVersionId("version-1");
        KitKnowledgePageVersion version = new KitKnowledgePageVersion()
                .setId("version-1")
                .setPageId("page-1")
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED);
        KitKnowledgePageBlock block = new KitKnowledgePageBlock()
                .setId("block-1")
                .setPageVersionId("version-1")
                .setOrderNo(1)
                .setBlockType(KnowledgeBlockType.PARAGRAPH)
                .setContent("内容");
        KitKnowledgePageSourceRef ref = new KitKnowledgePageSourceRef()
                .setId("ref-1")
                .setPageBlockId("block-1")
                .setSourceDocumentId("document-1");
        when(pageMapper.selectCurrentPagesBySourceDocumentId("document-1")).thenReturn(List.of(page));
        when(pageVersionMapper.selectById("version-1")).thenReturn(version);
        when(pageBlockMapper.selectByVersionId("version-1")).thenReturn(List.of(block));
        when(pageSourceRefMapper.selectByPageBlockIds(List.of("block-1"))).thenReturn(List.of(ref));

        service.removeSourceDocumentFromCurrentPages("document-1");

        verify(pageMapper).update(isNull(), any());
        verify(chunkIndexRepository).replacePageVersion("page-1", "version-1", List.of());
        verify(chunkBuilder, never()).build(any());
    }

    @Test
    void removeSourceDocumentFromCurrentPagesRebuildsPageWhenOtherContentStillExists() {
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgeChunkBuilder chunkBuilder = mock(KnowledgeChunkBuilder.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        KnowledgeSourceDocumentPageSyncService service = new KnowledgeSourceDocumentPageSyncService(
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper,
                chunkBuilder,
                chunkEmbeddingService,
                chunkIndexRepository);

        KitKnowledgePage page = new KitKnowledgePage()
                .setId("page-1")
                .setTitle("应急预案")
                .setPageStatus(KnowledgePageStatus.PUBLISHED)
                .setCurrentVersionId("version-1");
        KitKnowledgePageVersion version = new KitKnowledgePageVersion()
                .setId("version-1")
                .setPageId("page-1")
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED);
        KitKnowledgePageBlock removedBlock = new KitKnowledgePageBlock()
                .setId("block-1")
                .setPageVersionId("version-1")
                .setOrderNo(1)
                .setBlockType(KnowledgeBlockType.PARAGRAPH)
                .setContent("来自待删文档");
        KitKnowledgePageBlock remainedBlock = new KitKnowledgePageBlock()
                .setId("block-2")
                .setPageVersionId("version-1")
                .setOrderNo(2)
                .setBlockType(KnowledgeBlockType.PARAGRAPH)
                .setContent("保留内容");
        KitKnowledgePageSourceRef removedRef = new KitKnowledgePageSourceRef()
                .setId("ref-1")
                .setPageBlockId("block-1")
                .setSourceDocumentId("document-1");
        KitKnowledgePageSourceRef remainedRef = new KitKnowledgePageSourceRef()
                .setId("ref-2")
                .setPageBlockId("block-2")
                .setSourceDocumentId("document-2");
        List<KnowledgeChunkDocument> chunks = List.of(new KnowledgeChunkDocument().setChunkId("chunk-1"));
        when(pageMapper.selectCurrentPagesBySourceDocumentId("document-1")).thenReturn(List.of(page));
        when(pageVersionMapper.selectById("version-1")).thenReturn(version);
        when(pageVersionMapper.selectMaxVersionNo("page-1")).thenReturn(1);
        when(pageBlockMapper.selectByVersionId("version-1")).thenReturn(List.of(removedBlock, remainedBlock));
        when(pageSourceRefMapper.selectByPageBlockIds(List.of("block-1", "block-2"))).thenReturn(List.of(removedRef, remainedRef));
        when(chunkBuilder.build(any())).thenReturn(chunks);

        service.removeSourceDocumentFromCurrentPages("document-1");

        verify(pageVersionMapper).insert(any(KitKnowledgePageVersion.class));
        verify(pageBlockMapper).insert(any(KitKnowledgePageBlock.class));
        verify(pageSourceRefMapper).insert(any(KitKnowledgePageSourceRef.class));
        verify(chunkEmbeddingService).embedChunks(chunks);
        verify(chunkIndexRepository).replacePageVersion(eq("page-1"), any(String.class), eq(chunks));
    }
}
