package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.GeneratedKnowledgePage;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeBlockFactory;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.service.impl.KnowledgePageMergeServiceImpl;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgePageMergeServiceTest {

    private static final String TENANT_ID = "tenant-1";

    private static final String PAGE_ID = "page-1";

    @Test
    void concurrentPublishKeepsBlocksFromDifferentSourcesAndMovesCurrentVersion() throws Exception {
        InMemoryMergeFixture fixture = new InMemoryMergeFixture();
        KnowledgePageMergeService service = fixture.createService();

        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<String>> futures = pool.invokeAll(List.of(
                    () -> service.publish(TENANT_ID, document("document-a"), new GeneratedKnowledgePage("Wiki", "# 来源A")),
                    () -> service.publish(TENANT_ID, document("document-b"), new GeneratedKnowledgePage("Wiki", "# 来源B"))));
            for (Future<String> future : futures) {
                future.get();
            }
        }

        KnowledgePage currentPage = fixture.page();
        List<KnowledgePageVersion> versions = fixture.versions();
        List<KnowledgePageBlock> currentBlocks = fixture.blocksByVersion(currentPage.getCurrentVersionId());

        assertEquals(2, versions.size());
        assertEquals(List.of(1, 2), versions.stream().map(KnowledgePageVersion::getVersionNo).toList());
        assertTrue(versions.stream().allMatch(version -> version.getVersionStatus() == KnowledgePageVersionStatus.PUBLISHED));
        assertEquals(versions.getLast().getId(), currentPage.getCurrentVersionId());
        assertEquals(2, currentBlocks.size());
        assertEquals(new HashSet<>(List.of("document-a", "document-b")),
                new HashSet<>(fixture.sourceDocumentIds(currentBlocks)));
    }

    @Test
    void republishSameSourceReplacesOldBlocksOnly() {
        InMemoryMergeFixture fixture = new InMemoryMergeFixture();
        KnowledgePageMergeService service = fixture.createService();

        service.publish(TENANT_ID, document("document-a"), new GeneratedKnowledgePage("Wiki", "# 旧A"));
        service.publish(TENANT_ID, document("document-a"), new GeneratedKnowledgePage("Wiki", "# 新A"));

        KnowledgePage currentPage = fixture.page();
        List<KnowledgePageBlock> currentBlocks = fixture.blocksByVersion(currentPage.getCurrentVersionId());

        assertEquals(2, fixture.versions().size());
        assertEquals(1, currentBlocks.size());
        assertEquals("# 新A", currentBlocks.getFirst().getContent());
        assertEquals(List.of("document-a"), fixture.sourceDocumentIds(currentBlocks));
    }

    private static KnowledgeSourceDocument document(String documentId) {
        KnowledgeSourceDocument document = new KnowledgeSourceDocument()
                .setId(documentId)
                .setTargetPageId(PAGE_ID);
        document.setTenantId(TENANT_ID);
        return document;
    }

    private static final class InMemoryMergeFixture {

        private final ReentrantLock lock = new ReentrantLock();

        private final KnowledgePage page = new KnowledgePage()
                .setId(PAGE_ID)
                .setTitle("Wiki")
                .setPageStatus(KnowledgePageStatus.DRAFT);

        private final List<KnowledgePageVersion> versions = new ArrayList<>();

        private final Map<String, List<KnowledgePageBlock>> blocksByVersion = new ConcurrentHashMap<>();

        private final Map<String, KnowledgePageSourceRef> refByBlockId = new ConcurrentHashMap<>();

        private InMemoryMergeFixture() {
            page.setTenantId(TENANT_ID);
        }

        KnowledgePageMergeService createService() {
            CacheUtils cacheUtils = mock(CacheUtils.class);
            KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
            KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
            KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
            KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
            TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

            when(cacheUtils.executeWithLock(anyString(), any(Supplier.class))).thenAnswer(invocation -> {
                lock.lock();
                try {
                    return invocation.<Supplier<?>>getArgument(1).get();
                } finally {
                    lock.unlock();
                }
            });
            when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation ->
                    invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
            when(pageMapper.selectOne(any())).thenAnswer(invocation -> page);
            doAnswer(invocation -> {
                KnowledgePage update = invocation.getArgument(0);
                if (Objects.nonNull(update.getTitle())) {
                    page.setTitle(update.getTitle());
                }
                if (Objects.nonNull(update.getPageStatus())) {
                    page.setPageStatus(update.getPageStatus());
                }
                if (Objects.nonNull(update.getCurrentVersionId())) {
                    page.setCurrentVersionId(update.getCurrentVersionId());
                }
                return 1;
            }).when(pageMapper).update(any(KnowledgePage.class), any());
            when(pageVersionMapper.selectMaxVersionNo(TENANT_ID, PAGE_ID)).thenAnswer(invocation -> versions.size());
            doAnswer(invocation -> {
                KnowledgePageVersion version = invocation.getArgument(0);
                versions.add(version);
                blocksByVersion.put(version.getId(), new ArrayList<>());
                return 1;
            }).when(pageVersionMapper).insert(any(KnowledgePageVersion.class));
            when(pageBlockMapper.selectByVersionId(anyString(), anyString())).thenAnswer(invocation ->
                    new ArrayList<>(blocksByVersion.getOrDefault(invocation.getArgument(1), List.of())));
            doAnswer(invocation -> {
                KnowledgePageBlock block = invocation.getArgument(0);
                blocksByVersion.computeIfAbsent(block.getPageVersionId(), key -> new ArrayList<>()).add(block);
                return 1;
            }).when(pageBlockMapper).insert(any(KnowledgePageBlock.class));
            when(pageSourceRefMapper.selectByPageBlockIds(anyString(), any(Collection.class))).thenAnswer(invocation -> {
                Collection<String> blockIds = invocation.getArgument(1);
                return blockIds.stream()
                        .map(refByBlockId::get)
                        .filter(Objects::nonNull)
                        .toList();
            });
            doAnswer(invocation -> {
                KnowledgePageSourceRef ref = invocation.getArgument(0);
                refByBlockId.put(ref.getPageBlockId(), ref);
                return 1;
            }).when(pageSourceRefMapper).insert(any(KnowledgePageSourceRef.class));

            return new KnowledgePageMergeServiceImpl(
                    cacheUtils,
                    pageMapper,
                    pageVersionMapper,
                    pageBlockMapper,
                    pageSourceRefMapper,
                    new KnowledgeBlockFactory(),
                    transactionTemplate);
        }

        KnowledgePage page() {
            return page;
        }

        List<KnowledgePageVersion> versions() {
            return versions;
        }

        List<KnowledgePageBlock> blocksByVersion(String versionId) {
            return blocksByVersion.getOrDefault(versionId, List.of());
        }

        List<String> sourceDocumentIds(List<KnowledgePageBlock> blocks) {
            return blocks.stream()
                    .peek(block -> assertEquals(KnowledgeBlockType.HEADING, block.getBlockType()))
                    .map(block -> refByBlockId.get(block.getId()))
                    .map(KnowledgePageSourceRef::getSourceDocumentId)
                    .toList();
        }
    }
}
