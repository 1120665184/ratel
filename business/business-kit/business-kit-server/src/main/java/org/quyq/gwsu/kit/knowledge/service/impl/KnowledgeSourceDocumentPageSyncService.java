package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        for (KitKnowledgePage page : pageMapper.selectPagesBySourceDocumentId(sourceDocumentId)) {
            if (currentVersionContainsSourceDocument(page.getCurrentVersionId(), sourceDocumentId)) {
                pruneCurrentPage(page, sourceDocumentId);
            }
            purgeSourceDocumentHistory(page.getId(), sourceDocumentId);
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
            deletePage(page);
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
            deletePage(page);
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
                        .setSourceSegmentStartNo(snapshot.ref().getSourceSegmentStartNo())
                        .setSourceSegmentEndNo(snapshot.ref().getSourceSegmentEndNo())
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

    private boolean currentVersionContainsSourceDocument(String currentVersionId, String sourceDocumentId) {
        if (!StringUtils.hasText(currentVersionId) || !StringUtils.hasText(sourceDocumentId)) {
            return false;
        }
        List<KitKnowledgePageBlock> blocks = pageBlockMapper.selectByVersionId(currentVersionId);
        if (CollectionUtils.isEmpty(blocks)) {
            return false;
        }
        return loadRefs(blocks).stream()
                .anyMatch(ref -> Objects.equals(sourceDocumentId, ref.getSourceDocumentId()));
    }

    private void purgeSourceDocumentHistory(String pageId, String sourceDocumentId) {
        if (!StringUtils.hasText(pageId) || !StringUtils.hasText(sourceDocumentId)) {
            return;
        }
        List<KitKnowledgePageVersion> versions = pageVersionMapper.selectByPageId(pageId);
        if (CollectionUtils.isEmpty(versions)) {
            deleteOrphanPageIfPresent(pageId);
            return;
        }

        List<KitKnowledgePageBlock> blocks = pageBlockMapper.selectByVersionIds(
                versions.stream().map(KitKnowledgePageVersion::getId).toList());
        Map<String, List<KitKnowledgePageBlock>> blocksByVersionId = blocks.stream()
                .collect(Collectors.groupingBy(KitKnowledgePageBlock::getPageVersionId));
        Map<String, KitKnowledgePageSourceRef> refByBlockId = loadRefMap(blocks);

        Set<String> versionIdsToDelete = versions.stream()
                .filter(version -> containsSourceDocument(blocksByVersionId.get(version.getId()), refByBlockId, sourceDocumentId))
                .map(KitKnowledgePageVersion::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(versionIdsToDelete)) {
            deleteOrphanPageIfPresent(pageId);
            return;
        }

        KitKnowledgePage page = pageMapper.selectById(pageId);
        if (page != null && StringUtils.hasText(page.getCurrentVersionId())
                && versionIdsToDelete.contains(page.getCurrentVersionId())) {
            deletePage(page);
            return;
        }

        deletePageArtifacts(versionIdsToDelete);
        deleteOrphanPageIfPresent(pageId);
    }

    private boolean containsSourceDocument(List<KitKnowledgePageBlock> blocks,
                                           Map<String, KitKnowledgePageSourceRef> refByBlockId,
                                           String sourceDocumentId) {
        if (CollectionUtils.isEmpty(blocks)) {
            return false;
        }
        for (KitKnowledgePageBlock block : blocks) {
            KitKnowledgePageSourceRef ref = refByBlockId.get(block.getId());
            if (ref != null && Objects.equals(sourceDocumentId, ref.getSourceDocumentId())) {
                return true;
            }
        }
        return false;
    }

    private void deletePage(KitKnowledgePage page) {
        if (page == null) {
            return;
        }
        chunkIndexRepository.replacePageVersion(page.getId(), page.getCurrentVersionId(), List.of());
        deletePageArtifacts(pageVersionMapper.selectByPageId(page.getId()).stream()
                .map(KitKnowledgePageVersion::getId)
                .toList());
        pageMapper.deleteById(page.getId());
    }

    private void deleteOrphanPageIfPresent(String pageId) {
        if (!StringUtils.hasText(pageId)) {
            return;
        }
        KitKnowledgePage page = pageMapper.selectById(pageId);
        if (page == null) {
            return;
        }
        List<KitKnowledgePageVersion> remainingVersions = pageVersionMapper.selectByPageId(pageId);
        if (!CollectionUtils.isEmpty(remainingVersions)) {
            return;
        }
        chunkIndexRepository.replacePageVersion(pageId, page.getCurrentVersionId(), List.of());
        pageMapper.deleteById(pageId);
    }

    private void deletePageArtifacts(Collection<String> pageVersionIds) {
        if (CollectionUtils.isEmpty(pageVersionIds)) {
            return;
        }
        List<KitKnowledgePageBlock> blocks = pageBlockMapper.selectByVersionIds(pageVersionIds);
        List<String> blockIds = blocks.stream()
                .map(KitKnowledgePageBlock::getId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (!CollectionUtils.isEmpty(blockIds)) {
            pageSourceRefMapper.delete(new LambdaQueryWrapper<KitKnowledgePageSourceRef>()
                    .in(KitKnowledgePageSourceRef::getPageBlockId, blockIds));
            pageBlockMapper.delete(new LambdaQueryWrapper<KitKnowledgePageBlock>()
                    .in(KitKnowledgePageBlock::getId, blockIds));
        }
        pageVersionMapper.delete(new LambdaQueryWrapper<KitKnowledgePageVersion>()
                .in(KitKnowledgePageVersion::getId, pageVersionIds));
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

    private Map<String, KitKnowledgePageSourceRef> loadRefMap(Collection<KitKnowledgePageBlock> blocks) {
        if (CollectionUtils.isEmpty(blocks)) {
            return Map.of();
        }
        return loadRefs(new ArrayList<>(blocks)).stream()
                .collect(Collectors.toMap(KitKnowledgePageSourceRef::getPageBlockId, Function.identity(), (left, right) -> left));
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
                .sorted(Comparator.comparingInt(snapshot -> Objects.requireNonNullElse(snapshot.block().getOrderNo(), 0)))
                .map(snapshot -> snapshot.block().getContent())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private record BlockSnapshot(KitKnowledgePageBlock block, KitKnowledgePageSourceRef ref) {
    }
}
