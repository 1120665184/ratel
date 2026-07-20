package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgePageSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageBlockVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkDocument;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgePageCommandServiceImpl;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgePageCommandServiceTest {

    @Test
    void savePagePublishesNewVersionAndRebuildsChunks() {
        CacheUtils cacheUtils = mock(CacheUtils.class);
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgeSourceDocumentMapper sourceDocumentMapper = mock(KnowledgeSourceDocumentMapper.class);
        KnowledgeChunkBuilder chunkBuilder = mock(KnowledgeChunkBuilder.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        KnowledgePageCommandServiceImpl service = new KnowledgePageCommandServiceImpl(
                cacheUtils,
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper,
                sourceDocumentMapper,
                chunkBuilder,
                chunkEmbeddingService,
                chunkIndexRepository,
                transactionTemplate);

        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get())
                .when(cacheUtils).executeWithLock(any(String.class), any(Supplier.class));
        doAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                .doInTransaction(mock(TransactionStatus.class)))
                .when(transactionTemplate).execute(any());
        when(pageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new KitKnowledgePage()
                .setId("page-1")
                .setTitle("旧标题")
                .setPageStatus(KnowledgePageStatus.PUBLISHED)
                .setCurrentVersionId("version-old"));
        when(pageVersionMapper.selectMaxVersionNo("page-1")).thenReturn(3);
        when(pageBlockMapper.selectByVersionId("version-old")).thenReturn(List.of(
                existingBlock("block-1", 1),
                existingBlock("block-2", 2),
                existingBlock("block-3", 3)));
        when(pageSourceRefMapper.selectByPageBlockIds(any())).thenReturn(List.of(
                existingRef("block-1", "doc-1", "page:1"),
                existingRef("block-2", "doc-1", "page:1"),
                existingRef("block-3", "doc-2", "page:3")));
        when(sourceDocumentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                sourceDocument("doc-1"),
                sourceDocument("doc-2")));
        List<KnowledgeChunkDocument> chunks = List.of(
                new KnowledgeChunkDocument().setChunkId("chunk-1"),
                new KnowledgeChunkDocument().setChunkId("chunk-2"));
        when(chunkBuilder.build(any())).thenReturn(chunks);

        KnowledgePageSaveDTO dto = new KnowledgePageSaveDTO();
        dto.setId("page-1");
        dto.setTitle("知识首页");
        dto.setBlocks(List.of(
                block("block-1", KnowledgeBlockType.HEADING, "统一标题", "doc-1", "page:1"),
                block("block-2", KnowledgeBlockType.PARAGRAPH, "来源A内容", "doc-1", "page:1"),
                block("block-3", KnowledgeBlockType.PARAGRAPH, "来源B内容", "doc-2", "page:3")));

        String pageId = service.savePage(dto);

        assertEquals("page-1", pageId);
        verify(cacheUtils).executeWithLock(eq("knowledge:page:page-1"), any(Supplier.class));
        verify(pageVersionMapper).update(any(KitKnowledgePageVersion.class), any(LambdaUpdateWrapper.class));
        verify(pageVersionMapper).insert(any(KitKnowledgePageVersion.class));
        verify(pageBlockMapper, times(3)).insert(any(KitKnowledgePageBlock.class));
        verify(pageSourceRefMapper, times(3)).insert(any(KitKnowledgePageSourceRef.class));
        verify(chunkEmbeddingService).embedChunks(chunks);
        verify(chunkIndexRepository).replacePageVersion(eq("page-1"), any(String.class), eq(chunks));
    }

    @Test
    void savePageRejectsUnknownSourceDocument() {
        CacheUtils cacheUtils = mock(CacheUtils.class);
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgeSourceDocumentMapper sourceDocumentMapper = mock(KnowledgeSourceDocumentMapper.class);
        KnowledgeChunkBuilder chunkBuilder = mock(KnowledgeChunkBuilder.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        KnowledgePageCommandServiceImpl service = new KnowledgePageCommandServiceImpl(
                cacheUtils,
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper,
                sourceDocumentMapper,
                chunkBuilder,
                chunkEmbeddingService,
                chunkIndexRepository,
                transactionTemplate);

        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get())
                .when(cacheUtils).executeWithLock(any(String.class), any(Supplier.class));
        doAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                .doInTransaction(mock(TransactionStatus.class)))
                .when(transactionTemplate).execute(any());
        when(sourceDocumentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        KnowledgePageSaveDTO dto = new KnowledgePageSaveDTO();
        dto.setTitle("知识首页");
        dto.setBlocks(List.of(block("block-new", KnowledgeBlockType.PARAGRAPH, "内容", "doc-missing", "page:1")));

        assertThrows(BusinessException.class, () -> service.savePage(dto));
        verify(chunkBuilder, never()).build(any());
    }

    @Test
    void savePageRejectsSourceMutationForExistingPage() {
        KnowledgePageCommandServiceImpl service = serviceForExistingPage();

        KnowledgePageSaveDTO dto = new KnowledgePageSaveDTO();
        dto.setId("page-1");
        dto.setTitle("知识首页");
        dto.setBlocks(List.of(
                block("block-1", KnowledgeBlockType.HEADING, "统一标题", "doc-2", "page:1"),
                block("block-2", KnowledgeBlockType.PARAGRAPH, "来源A内容", "doc-1", "page:1")));

        assertThrows(BusinessException.class, () -> service.savePage(dto));
    }

    @Test
    void savePageRejectsNewBlockForExistingPage() {
        KnowledgePageCommandServiceImpl service = serviceForExistingPage();

        KnowledgePageSaveDTO dto = new KnowledgePageSaveDTO();
        dto.setId("page-1");
        dto.setTitle("知识首页");
        dto.setBlocks(List.of(
                block("block-1", KnowledgeBlockType.HEADING, "统一标题", "doc-1", "page:1"),
                block("block-new", KnowledgeBlockType.PARAGRAPH, "新增内容", "doc-1", "page:1")));

        assertThrows(BusinessException.class, () -> service.savePage(dto));
    }

    private static KnowledgePageBlockVO block(String id,
                                              KnowledgeBlockType blockType,
                                              String content,
                                              String sourceDocumentId,
                                              String sourceLocator) {
        return new KnowledgePageBlockVO()
                .setId(id)
                .setBlockType(blockType)
                .setContent(content)
                .setSourceType(KnowledgeSourceType.SOURCE_DOCUMENT)
                .setSourceDocumentId(sourceDocumentId)
                .setSourceLocator(sourceLocator);
    }

    private static KnowledgePageBlockVO block(KnowledgeBlockType blockType,
                                              String content,
                                              String sourceDocumentId,
                                              String sourceLocator) {
        return block(null, blockType, content, sourceDocumentId, sourceLocator);
    }

    private static KnowledgePageCommandServiceImpl serviceForExistingPage() {
        CacheUtils cacheUtils = mock(CacheUtils.class);
        KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgeSourceDocumentMapper sourceDocumentMapper = mock(KnowledgeSourceDocumentMapper.class);
        KnowledgeChunkBuilder chunkBuilder = mock(KnowledgeChunkBuilder.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        KnowledgePageCommandServiceImpl service = new KnowledgePageCommandServiceImpl(
                cacheUtils,
                pageMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper,
                sourceDocumentMapper,
                chunkBuilder,
                chunkEmbeddingService,
                chunkIndexRepository,
                transactionTemplate);

        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get())
                .when(cacheUtils).executeWithLock(any(String.class), any(Supplier.class));
        doAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                .doInTransaction(mock(TransactionStatus.class)))
                .when(transactionTemplate).execute(any());
        when(pageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new KitKnowledgePage()
                .setId("page-1")
                .setTitle("旧标题")
                .setPageStatus(KnowledgePageStatus.PUBLISHED)
                .setCurrentVersionId("version-old"));
        when(pageBlockMapper.selectByVersionId("version-old")).thenReturn(List.of(
                existingBlock("block-1", 1),
                existingBlock("block-2", 2)));
        when(pageSourceRefMapper.selectByPageBlockIds(any())).thenReturn(List.of(
                existingRef("block-1", "doc-1", "page:1"),
                existingRef("block-2", "doc-1", "page:1")));
        when(sourceDocumentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sourceDocument("doc-1")));
        return service;
    }

    private static KitKnowledgePageBlock existingBlock(String id, int orderNo) {
        return new KitKnowledgePageBlock()
                .setId(id)
                .setPageVersionId("version-old")
                .setOrderNo(orderNo)
                .setBlockType(KnowledgeBlockType.PARAGRAPH)
                .setContent("旧内容");
    }

    private static KitKnowledgePageSourceRef existingRef(String blockId,
                                                         String sourceDocumentId,
                                                         String sourceLocator) {
        return new KitKnowledgePageSourceRef()
                .setId("ref-" + blockId)
                .setPageBlockId(blockId)
                .setSourceType(KnowledgeSourceType.SOURCE_DOCUMENT)
                .setSourceDocumentId(sourceDocumentId)
                .setSourceLocator(sourceLocator);
    }

    private static KitKnowledgeSourceDocument sourceDocument(String id) {
        return new KitKnowledgeSourceDocument().setId(id);
    }
}
