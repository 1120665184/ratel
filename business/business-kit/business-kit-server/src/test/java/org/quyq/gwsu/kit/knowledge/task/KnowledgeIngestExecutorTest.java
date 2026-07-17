package org.quyq.gwsu.kit.knowledge.task;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeIngestTask;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.GeneratedKnowledgePage;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkDocument;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeDocumentParser;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageGenerator;
import org.quyq.gwsu.kit.knowledge.engine.ParsedKnowledgeDocument;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeIngestTaskMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.KnowledgePageMergeService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeIngestExecutorTest {

    @Test
    void executeBuildsEmbedsAndIndexesChunksAfterPageMerge() {
        KnowledgeIngestTaskMapper ingestTaskMapper = mock(KnowledgeIngestTaskMapper.class);
        KnowledgeSourceDocumentMapper sourceDocumentMapper = mock(KnowledgeSourceDocumentMapper.class);
        KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
        KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
        KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
        KnowledgeDocumentParser documentParser = mock(KnowledgeDocumentParser.class);
        KnowledgePageGenerator pageGenerator = mock(KnowledgePageGenerator.class);
        KnowledgePageMergeService pageMergeService = mock(KnowledgePageMergeService.class);
        KnowledgeChunkBuilder chunkBuilder = mock(KnowledgeChunkBuilder.class);
        KnowledgeChunkEmbeddingService chunkEmbeddingService = mock(KnowledgeChunkEmbeddingService.class);
        KnowledgeChunkIndexRepository chunkIndexRepository = mock(KnowledgeChunkIndexRepository.class);
        KnowledgeIngestExecutor executor = new KnowledgeIngestExecutor(
                ingestTaskMapper,
                sourceDocumentMapper,
                pageVersionMapper,
                pageBlockMapper,
                pageSourceRefMapper,
                documentParser,
                pageGenerator,
                pageMergeService,
                chunkBuilder,
                chunkEmbeddingService,
                chunkIndexRepository);

        KitKnowledgeIngestTask task = new KitKnowledgeIngestTask()
                .setId("task-1")
                .setSourceDocumentId("document-1");
        task.setTenantId("tenant-1");
        KitKnowledgeSourceDocument sourceDocument = new KitKnowledgeSourceDocument()
                .setId("document-1")
                .setFileId("file-1");
        sourceDocument.setTenantId("tenant-1");
        GeneratedKnowledgePage page = new GeneratedKnowledgePage("标题", "# 标题");
        KitKnowledgePageVersion version = new KitKnowledgePageVersion()
                .setId("version-1")
                .setPageId("page-1")
                .setVersionNo(1);
        version.setTenantId("tenant-1");
        KitKnowledgePageBlock block = new KitKnowledgePageBlock()
                .setId("block-1")
                .setPageVersionId("version-1")
                .setBlockType(KnowledgeBlockType.HEADING)
                .setContent("# 标题")
                .setOrderNo(1);
        KitKnowledgePageSourceRef ref = new KitKnowledgePageSourceRef()
                .setPageBlockId("block-1")
                .setSourceDocumentId("document-1");
        List<KnowledgeChunkDocument> chunks = List.of(new KnowledgeChunkDocument()
                .setChunkId("chunk-1")
                .setPageId("page-1")
                .setPageVersionId("version-1")
                .setPageBlockId("block-1")
                .setSourceDocumentId("document-1")
                .setContent("# 标题")
                .setChunkOrder(1));

        when(ingestTaskMapper.selectOne(any())).thenReturn(task);
        when(sourceDocumentMapper.selectOne(any())).thenReturn(sourceDocument);
        when(documentParser.parse("file-1")).thenReturn(ParsedKnowledgeDocument.of("file.md", "正文"));
        when(pageGenerator.generate("file.md", "正文")).thenReturn(page);
        when(pageMergeService.publish("tenant-1", sourceDocument, page)).thenReturn("version-1");
        when(pageVersionMapper.selectOne(any())).thenReturn(version);
        when(pageBlockMapper.selectByVersionId("tenant-1", "version-1")).thenReturn(List.of(block));
        when(pageSourceRefMapper.selectByPageBlockIds(eq("tenant-1"), any())).thenReturn(List.of(ref));
        when(chunkBuilder.build(any())).thenReturn(chunks);

        executor.execute("task-1");

        verify(chunkEmbeddingService).embedChunks(chunks);
        verify(chunkIndexRepository).replacePageVersion("page-1", "version-1", chunks);
    }
}
