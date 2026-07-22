package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuildRequest;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkDocument;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 知识源文档与 Wiki Page 同步服务。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeSourceDocumentPageSyncService {

    private final KnowledgePageMapper pageMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    private final KnowledgePageBlockMapper pageBlockMapper;

    private final KnowledgePageSourceRefMapper pageSourceRefMapper;

    private final KnowledgeChunkBuilder chunkBuilder;

    private final KnowledgeChunkEmbeddingService chunkEmbeddingService;

    private final KnowledgeChunkIndexRepository chunkIndexRepository;

    public void reindexCurrentPagesBySourceDocumentId(String sourceDocumentId) {
        if (!StringUtils.hasText(sourceDocumentId)) {
            return;
        }
        for (KitKnowledgePage page : pageMapper.selectCurrentPagesBySourceDocumentId(sourceDocumentId)) {
            reindexCurrentPage(page);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeSourceDocumentFromCurrentPages(String sourceDocumentId) {
        if (!StringUtils.hasText(sourceDocumentId)) {
            return;
        }
        for (KitKnowledgePage page : pageMapper.selectCurrentPagesBySourceDocumentId(sourceDocumentId)) {
            pruneCurrentPage(page, sourceDocumentId);
        }
    }

    private void reindexCurrentPage(KitKnowledgePage page) {
        if (page == null || !StringUtils.hasText(page.getCurrentVersionId())) {
            return;
        }
        KitKnowledgePageVersion currentVersion = loadCurrentVersion(page.getCurrentVersionId());
        if (currentVersion == null) {
            return;
        }
        List<KitKnowledgePageBlock> blocks = pageBlockMapper.selectByVersionId(currentVersion.getId());
        if (CollectionUtils.isEmpty(blocks)) {
            chunkIndexRepository.replacePageVersion(page.getId(), currentVersion.getId(), List.of());
            return;
        }
        List<KitKnowledgePageSourceRef> refs = loadRefs(blocks);
        replacePageVersion(page, currentVersion, blocks, refs);
    }

    private void pruneCurrentPage(KitKnowledgePage page, String sourceDocumentId) {
        if (page == null || !StringUtils.hasText(page.getCurrentVersionId())) {
            return;
        }
        KitKnowledgePageVersion currentVersion = loadCurrentVersion(page.getCurrentVersionId());
        if (currentVersion == null) {
            return;
        }

        List<KitKnowledgePageBlock> currentBlocks = pageBlockMapper.selectByVersionId(currentVersion.getId());
        if (CollectionUtils.isEmpty(currentBlocks)) {
            deletePage(page, currentVersion.getId());
            return;
        }

        Map<String, KitKnowledgePageSourceRef> refByBlockId = loadRefMap(currentBlocks);
        List<BlockSnapshot> remainedBlocks = new ArrayList<>();
        for (KitKnowledgePageBlock block : currentBlocks) {
            KitKnowledgePageSourceRef ref = refByBlockId.get(block.getId());
            if (ref != null && Objects.equals(sourceDocumentId, ref.getSourceDocumentId())) {
                continue;
            }
            remainedBlocks.add(new BlockSnapshot(block, ref));
        }
        if (!hasContentBlock(remainedBlocks)) {
            deletePage(page, currentVersion.getId());
            return;
        }

        archiveCurrentVersion(currentVersion.getId());

        String newVersionId = IdWorker.getIdStr();
        KitKnowledgePageVersion newVersion = new KitKnowledgePageVersion()
                .setId(newVersionId)
                .setPageId(page.getId())
                .setVersionNo(Objects.requireNonNullElse(pageVersionMapper.selectMaxVersionNo(page.getId()), 0) + 1)
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED)
                .setMarkdownContent(renderMarkdown(remainedBlocks))
                .setPublishedAt(LocalDateTime.now());
        pageVersionMapper.insert(newVersion);

        List<KitKnowledgePageBlock> newBlocks = new ArrayList<>(remainedBlocks.size());
        List<KitKnowledgePageSourceRef> newRefs = new ArrayList<>(remainedBlocks.size());
        for (int i = 0; i < remainedBlocks.size(); i++) {
            BlockSnapshot snapshot = remainedBlocks.get(i);
            String blockId = IdWorker.getIdStr();
            KitKnowledgePageBlock newBlock = new KitKnowledgePageBlock()
                    .setId(blockId)
                    .setPageVersionId(newVersionId)
                    .setOrderNo(i + 1)
                    .setBlockType(snapshot.block().getBlockType())
                    .setContent(snapshot.block().getContent());
            newBlocks.add(newBlock);
            if (snapshot.ref() != null) {
                newRefs.add(new KitKnowledgePageSourceRef()
                        .setId(IdWorker.getIdStr())
                        .setPageBlockId(blockId)
                        .setSourceType(snapshot.ref().getSourceType())
                        .setSourceDocumentId(snapshot.ref().getSourceDocumentId())
                        .setSourceLocator(snapshot.ref().getSourceLocator()));
            }
        }
        newBlocks.forEach(pageBlockMapper::insert);
        newRefs.forEach(pageSourceRefMapper::insert);

        pageMapper.update(null, new LambdaUpdateWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, page.getId())
                .eq(KitKnowledgePage::getDeleted, false)
                .set(KitKnowledgePage::getCurrentVersionId, newVersionId)
                .set(KitKnowledgePage::getPageStatus, KnowledgePageStatus.PUBLISHED));

        replacePageVersion(page, newVersion, newBlocks, newRefs);
    }

    private void deletePage(KitKnowledgePage page, String currentVersionId) {
        archiveCurrentVersion(currentVersionId);
        pageMapper.update(null, new LambdaUpdateWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, page.getId())
                .eq(KitKnowledgePage::getDeleted, false)
                .set(KitKnowledgePage::getDeleted, true)
                .set(KitKnowledgePage::getCurrentVersionId, null)
                .set(KitKnowledgePage::getPageStatus, KnowledgePageStatus.ARCHIVED));
        chunkIndexRepository.replacePageVersion(page.getId(), currentVersionId, List.of());
    }

    private void replacePageVersion(KitKnowledgePage page,
                                    KitKnowledgePageVersion pageVersion,
                                    List<KitKnowledgePageBlock> blocks,
                                    List<KitKnowledgePageSourceRef> refs) {
        List<KnowledgeChunkDocument> chunks = chunkBuilder.build(new KnowledgeChunkBuildRequest(
                page.getId(),
                page.getTitle(),
                pageVersion,
                blocks,
                refs));
        chunkEmbeddingService.embedChunks(chunks);
        chunkIndexRepository.replacePageVersion(page.getId(), pageVersion.getId(), chunks);
    }

    private KitKnowledgePageVersion loadCurrentVersion(String currentVersionId) {
        if (!StringUtils.hasText(currentVersionId)) {
            return null;
        }
        return pageVersionMapper.selectById(currentVersionId);
    }

    private List<KitKnowledgePageSourceRef> loadRefs(List<KitKnowledgePageBlock> blocks) {
        if (CollectionUtils.isEmpty(blocks)) {
            return List.of();
        }
        return pageSourceRefMapper.selectByPageBlockIds(blocks.stream()
                .map(KitKnowledgePageBlock::getId)
                .toList());
    }

    private Map<String, KitKnowledgePageSourceRef> loadRefMap(List<KitKnowledgePageBlock> blocks) {
        Map<String, KitKnowledgePageSourceRef> refByBlockId = new HashMap<>();
        loadRefs(blocks).forEach(ref -> refByBlockId.put(ref.getPageBlockId(), ref));
        return refByBlockId;
    }

    private boolean hasContentBlock(List<BlockSnapshot> blocks) {
        return blocks.stream().anyMatch(snapshot -> snapshot.block().getBlockType() != KnowledgeBlockType.HEADING);
    }

    private void archiveCurrentVersion(String currentVersionId) {
        if (!StringUtils.hasText(currentVersionId)) {
            return;
        }
        pageVersionMapper.update(null, new LambdaUpdateWrapper<KitKnowledgePageVersion>()
                .eq(KitKnowledgePageVersion::getId, currentVersionId)
                .eq(KitKnowledgePageVersion::getDeleted, false)
                .set(KitKnowledgePageVersion::getVersionStatus, KnowledgePageVersionStatus.ARCHIVED));
    }

    private String renderMarkdown(List<BlockSnapshot> blocks) {
        return blocks.stream()
                .map(snapshot -> snapshot.block().getContent())
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private record BlockSnapshot(KitKnowledgePageBlock block, KitKnowledgePageSourceRef ref) {
    }
}
