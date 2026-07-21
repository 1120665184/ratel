package org.quyq.gwsu.kit.knowledge.service;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.GeneratedKnowledgePage;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeBlockFactory;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageCandidateService;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageMatchAction;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageMatchDecision;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageMergeModelClient;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageMergePlan;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgePageMergePromptBuilder;
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
                    () -> service.publish(document("document-a"), new GeneratedKnowledgePage("Wiki", "# Wiki\n\n来源A 内容")),
                    () -> service.publish(document("document-b"), new GeneratedKnowledgePage("Wiki", "# Wiki\n\n来源B 内容"))));
            for (Future<String> future : futures) {
                future.get();
            }
        }

        KitKnowledgePage currentPage = fixture.page();
        List<KitKnowledgePageVersion> versions = fixture.versions();
        List<KitKnowledgePageBlock> currentBlocks = fixture.blocksByVersion(currentPage.getCurrentVersionId());

        assertEquals(2, versions.size());
        assertEquals(List.of(1, 2), versions.stream().map(KitKnowledgePageVersion::getVersionNo).toList());
        assertTrue(versions.stream().allMatch(version -> version.getVersionStatus() == KnowledgePageVersionStatus.PUBLISHED));
        assertEquals(versions.getLast().getId(), currentPage.getCurrentVersionId());
        assertEquals(3, currentBlocks.size());
        assertEquals(new HashSet<>(List.of("document-a", "document-b")),
                new HashSet<>(fixture.sourceDocumentIds(currentBlocks)));
    }

    @Test
    void republishSameSourceReplacesOldBlocksOnly() {
        InMemoryMergeFixture fixture = new InMemoryMergeFixture();
        KnowledgePageMergeService service = fixture.createService();

        service.publish(document("document-a"), new GeneratedKnowledgePage("Wiki", "# Wiki\n\n旧A 内容"));
        service.publish(document("document-a"), new GeneratedKnowledgePage("Wiki", "# Wiki\n\n新A 内容"));

        KitKnowledgePage currentPage = fixture.page();
        List<KitKnowledgePageBlock> currentBlocks = fixture.blocksByVersion(currentPage.getCurrentVersionId());

        assertEquals(2, fixture.versions().size());
        assertEquals(2, currentBlocks.size());
        assertTrue(currentBlocks.stream().anyMatch(block -> "新A 内容".equals(block.getContent())));
        assertEquals(List.of("document-a"), fixture.sourceDocumentIds(currentBlocks));
    }

    private static KitKnowledgeSourceDocument document(String documentId) {
        return new KitKnowledgeSourceDocument()
                .setId(documentId);
    }

    private static final class InMemoryMergeFixture {

        private final ReentrantLock lock = new ReentrantLock();

        private final KitKnowledgePage page = new KitKnowledgePage()
                .setId(PAGE_ID)
                .setTitle("Wiki")
                .setPageStatus(KnowledgePageStatus.DRAFT);

        private final List<KitKnowledgePageVersion> versions = new ArrayList<>();

        private final Map<String, List<KitKnowledgePageBlock>> blocksByVersion = new ConcurrentHashMap<>();

        private final Map<String, KitKnowledgePageSourceRef> refByBlockId = new ConcurrentHashMap<>();

        private InMemoryMergeFixture() {
        }

        KnowledgePageMergeService createService() {
            CacheUtils cacheUtils = mock(CacheUtils.class);
            KnowledgePageMapper pageMapper = mock(KnowledgePageMapper.class);
            KnowledgePageVersionMapper pageVersionMapper = mock(KnowledgePageVersionMapper.class);
            KnowledgePageBlockMapper pageBlockMapper = mock(KnowledgePageBlockMapper.class);
            KnowledgePageSourceRefMapper pageSourceRefMapper = mock(KnowledgePageSourceRefMapper.class);
            KnowledgePageCandidateService candidateService = mock(KnowledgePageCandidateService.class);
            KnowledgePageMergePromptBuilder promptBuilder = mock(KnowledgePageMergePromptBuilder.class);
            KnowledgePageMergeModelClient mergeModelClient = mock(KnowledgePageMergeModelClient.class);
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
                KitKnowledgePage update = invocation.getArgument(0);
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
            }).when(pageMapper).update(any(KitKnowledgePage.class), any());
            when(pageVersionMapper.selectMaxVersionNo(PAGE_ID)).thenAnswer(invocation -> versions.size());
            doAnswer(invocation -> {
                KitKnowledgePageVersion version = invocation.getArgument(0);
                versions.add(version);
                blocksByVersion.put(version.getId(), new ArrayList<>());
                return 1;
            }).when(pageVersionMapper).insert(any(KitKnowledgePageVersion.class));
            when(pageBlockMapper.selectByVersionId(anyString())).thenAnswer(invocation ->
                    new ArrayList<>(blocksByVersion.getOrDefault(invocation.getArgument(0), List.of())));
            doAnswer(invocation -> {
                KitKnowledgePageBlock block = invocation.getArgument(0);
                blocksByVersion.computeIfAbsent(block.getPageVersionId(), key -> new ArrayList<>()).add(block);
                return 1;
            }).when(pageBlockMapper).insert(any(KitKnowledgePageBlock.class));
            when(pageSourceRefMapper.selectByPageBlockIds(any(Collection.class))).thenAnswer(invocation -> {
                Collection<String> blockIds = invocation.getArgument(0);
                return blockIds.stream()
                        .map(refByBlockId::get)
                        .filter(Objects::nonNull)
                        .toList();
            });
            doAnswer(invocation -> {
                KitKnowledgePageSourceRef ref = invocation.getArgument(0);
                refByBlockId.put(ref.getPageBlockId(), ref);
                return 1;
            }).when(pageSourceRefMapper).insert(any(KitKnowledgePageSourceRef.class));
            when(promptBuilder.buildMatchPrompt(anyString(), any(), any())).thenReturn("match-prompt");
            when(mergeModelClient.matchPage(anyString(), any())).thenReturn(
                    new KnowledgePageMatchDecision(KnowledgePageMatchAction.MATCH_EXISTING_PAGE, PAGE_ID, 1.0D, "测试固定 Page"));
            when(promptBuilder.buildMergePlanPrompt(anyString(), anyString(), any(), any())).thenReturn("merge-prompt");
            when(mergeModelClient.planMerge(anyString())).thenReturn(new KnowledgePageMergePlan("Wiki", List.of(
                    new KnowledgePageMergePlan.Item("HEADING", null, "# Wiki"),
                    new KnowledgePageMergePlan.Item("EXISTING_BLOCK", "E2", null),
                    new KnowledgePageMergePlan.Item("INCOMING_BLOCK", "I2", null))));

            return new KnowledgePageMergeServiceImpl(
                    cacheUtils,
                    pageMapper,
                    pageVersionMapper,
                    pageBlockMapper,
                    pageSourceRefMapper,
                    new KnowledgeBlockFactory(),
                    new KnowledgeProperties(),
                    candidateService,
                    promptBuilder,
                    mergeModelClient,
                    transactionTemplate);
        }

        KitKnowledgePage page() {
            return page;
        }

        List<KitKnowledgePageVersion> versions() {
            return versions;
        }

        List<KitKnowledgePageBlock> blocksByVersion(String versionId) {
            return blocksByVersion.getOrDefault(versionId, List.of());
        }

        List<String> sourceDocumentIds(List<KitKnowledgePageBlock> blocks) {
            return blocks.stream()
                    .filter(block -> block.getBlockType() != KnowledgeBlockType.HEADING)
                    .map(block -> refByBlockId.get(block.getId()))
                    .map(KitKnowledgePageSourceRef::getSourceDocumentId)
                    .toList();
        }
    }
}
