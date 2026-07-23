package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.page.GeneratedKnowledgePage;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgeBlockBuildResult;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgeBlockFactory;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgePageCandidate;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgePageCandidateService;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgePageMatchDecision;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgePageMergeBlockRef;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgePageMergeModelClient;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgePageMergePlan;
import org.quyq.gwsu.kit.knowledge.engine.page.KnowledgePageMergePromptBuilder;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.service.KnowledgePageMergeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识 Page 合并发布服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgePageMergeServiceImpl implements KnowledgePageMergeService {

    private final CacheUtils cacheUtils;

    private final KnowledgePageMapper pageMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    private final KnowledgePageBlockMapper pageBlockMapper;

    private final KnowledgePageSourceRefMapper pageSourceRefMapper;

    private final KnowledgeBlockFactory blockFactory;

    private final KnowledgeProperties knowledgeProperties;

    private final KnowledgePageCandidateService candidateService;

    private final KnowledgePageMergePromptBuilder promptBuilder;

    private final KnowledgePageMergeModelClient mergeModelClient;

    private final TransactionTemplate transactionTemplate;

    @Override
    public String publish(KitKnowledgeSourceDocument sourceDocument, GeneratedKnowledgePage generatedPage) {
        AssertUtils.hasText(sourceDocument.getId(), KitErrorCode.E03009);
        AssertUtils.hasText(generatedPage.title(), KitErrorCode.E03009);
        AssertUtils.hasText(generatedPage.markdownContent(), KitErrorCode.E03009);
        MergeTarget target = resolveMergeTarget(sourceDocument, generatedPage);
        return cacheUtils.executeWithLock(target.lockKey(), () -> {
            String resolvedPageId = resolvePageIdInLock(target, generatedPage);
            return transactionTemplate.execute(status -> publishLocked(
                    resolvedPageId,
                    sourceDocument,
                    generatedPage));
        });
    }

    private String publishLocked(String pageId,
                                 KitKnowledgeSourceDocument sourceDocument,
                                 GeneratedKnowledgePage generatedPage) {
        KitKnowledgePage page = ensurePage(pageId, normalizeTitle(generatedPage.title()));
        archiveCurrentVersion(page.getCurrentVersionId());
        String newVersionId = IdWorker.getIdStr();
        int nextVersionNo = Objects.requireNonNullElse(pageVersionMapper.selectMaxVersionNo(page.getId()), 0) + 1;
        KnowledgeBlockBuildResult newSourceBlocks = blockFactory.build(
                newVersionId,
                sourceDocument.getId(),
                generatedPage.markdownContent());
        validateSingleSource(sourceDocument.getId(), newSourceBlocks);
        MergeResult mergeResult = mergeBlocks(page, newVersionId, sourceDocument.getId(), newSourceBlocks, generatedPage);
        KitKnowledgePageVersion newVersion = new KitKnowledgePageVersion()
                .setId(newVersionId)
                .setPageId(page.getId())
                .setVersionNo(nextVersionNo)
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED)
                .setMarkdownContent(mergeResult.markdownContent())
                .setPublishedAt(LocalDateTime.now());
        pageVersionMapper.insert(newVersion);

        mergeResult.blocks().forEach(pageBlockMapper::insert);
        mergeResult.sourceRefs().forEach(pageSourceRefMapper::insert);

        KitKnowledgePage update = new KitKnowledgePage()
                .setTitle(mergeResult.title())
                .setPageStatus(KnowledgePageStatus.PUBLISHED)
                .setCurrentVersionId(newVersionId);
        pageMapper.update(update, new LambdaUpdateWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, page.getId())
                .eq(KitKnowledgePage::getDeleted, false));
        return newVersionId;
    }

    private MergeTarget resolveMergeTarget(KitKnowledgeSourceDocument sourceDocument, GeneratedKnowledgePage generatedPage) {
        KnowledgePageMatchDecision decision = matchExistingPage(generatedPage);
        if (decision.matchedExistingPage()) {
            return new MergeTarget(decision.pageId(), "knowledge:page:id:" + decision.pageId(), false);
        }
        return new MergeTarget(null, "knowledge:page:title:" + normalizeTitle(generatedPage.title()), true);
    }

    private String resolvePageIdInLock(MergeTarget target, GeneratedKnowledgePage generatedPage) {
        if (StringUtils.hasText(target.pageId())) {
            return target.pageId();
        }
        if (target.recheckOnCreate()) {
            KnowledgePageMatchDecision decision = matchExistingPage(generatedPage);
            if (decision.matchedExistingPage()) {
                return decision.pageId();
            }
        }
        return IdWorker.getIdStr();
    }

    private KnowledgePageMatchDecision matchExistingPage(GeneratedKnowledgePage generatedPage) {
        List<KnowledgePageCandidate> candidates = candidateService.recall(generatedPage);
        return mergeModelClient.matchPage(
                promptBuilder.buildMatchPrompt(knowledgeProperties.getWikiOutputLanguage(), generatedPage, candidates),
                candidates);
    }

    private String normalizeTitle(String title) {
        return title.trim();
    }

    private String normalizeHeading(String content) {
        String trimmed = content.trim();
        return trimmed.startsWith("#") ? trimmed : "## " + trimmed;
    }

    private KitKnowledgePage ensurePage(String pageId, String title) {
        KitKnowledgePage page = pageMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, pageId)
                .eq(KitKnowledgePage::getDeleted, false));
        if (Objects.nonNull(page)) {
            return page;
        }
        KitKnowledgePage created = new KitKnowledgePage()
                .setId(pageId)
                .setTitle(title)
                .setPageStatus(KnowledgePageStatus.DRAFT);
        pageMapper.insert(created);
        return created;
    }

    private MergeResult mergeBlocks(KitKnowledgePage page,
                                    String newVersionId,
                                    String replacingSourceDocumentId,
                                    KnowledgeBlockBuildResult incomingBuildResult,
                                    GeneratedKnowledgePage generatedPage) {
        List<KnowledgePageMergeBlockRef> existingRefs = loadExistingBlockRefs(page.getCurrentVersionId(), replacingSourceDocumentId);
        List<KnowledgePageMergeBlockRef> incomingRefs = toIncomingRefs(incomingBuildResult);
        if (CollectionUtils.isEmpty(existingRefs)) {
            List<KitKnowledgePageBlock> blocks = copyRefsToBlocks(newVersionId, incomingRefs);
            List<KitKnowledgePageSourceRef> refs = copyRefsToSourceRefs(blocks, incomingRefs);
            resetOrderNo(blocks);
            return new MergeResult(normalizeTitle(generatedPage.title()), blocks, refs, renderMarkdown(blocks));
        }
        KnowledgePageMergePlan plan = mergeModelClient.planMerge(promptBuilder.buildMergePlanPrompt(
                knowledgeProperties.getWikiOutputLanguage(),
                page.getTitle(),
                existingRefs,
                incomingRefs));
        return applyMergePlan(newVersionId, page.getTitle(), plan, existingRefs, incomingRefs);
    }

    private List<KnowledgePageMergeBlockRef> loadExistingBlockRefs(String currentVersionId, String replacingSourceDocumentId) {
        if (!StringUtils.hasText(currentVersionId)) {
            return List.of();
        }
        List<KitKnowledgePageBlock> currentBlocks = pageBlockMapper.selectByVersionId(currentVersionId);
        if (CollectionUtils.isEmpty(currentBlocks)) {
            return List.of();
        }
        Map<String, KitKnowledgePageSourceRef> refByBlockId = pageSourceRefMapper
                .selectByPageBlockIds(currentBlocks.stream().map(KitKnowledgePageBlock::getId).toList())
                .stream()
                .collect(Collectors.toMap(KitKnowledgePageSourceRef::getPageBlockId, Function.identity(), (left, right) -> left));
        List<KnowledgePageMergeBlockRef> refs = new ArrayList<>();
        int index = 1;
        for (KitKnowledgePageBlock block : currentBlocks) {
            KitKnowledgePageSourceRef sourceRef = refByBlockId.get(block.getId());
            if (sourceRef != null && replacingSourceDocumentId.equals(sourceRef.getSourceDocumentId())) {
                continue;
            }
            refs.add(new KnowledgePageMergeBlockRef("E" + index++, block, sourceRef));
        }
        return refs;
    }

    private List<KnowledgePageMergeBlockRef> toIncomingRefs(KnowledgeBlockBuildResult incomingBuildResult) {
        Map<String, KitKnowledgePageSourceRef> refByBlockId = incomingBuildResult.sourceRefs().stream()
                .collect(Collectors.toMap(KitKnowledgePageSourceRef::getPageBlockId, Function.identity(), (left, right) -> left));
        List<KnowledgePageMergeBlockRef> refs = new ArrayList<>();
        int index = 1;
        for (KitKnowledgePageBlock block : incomingBuildResult.blocks()) {
            refs.add(new KnowledgePageMergeBlockRef("I" + index++, block, refByBlockId.get(block.getId())));
        }
        return refs;
    }

    private MergeResult applyMergePlan(String newVersionId,
                                       String fallbackTitle,
                                       KnowledgePageMergePlan plan,
                                       List<KnowledgePageMergeBlockRef> existingRefs,
                                       List<KnowledgePageMergeBlockRef> incomingRefs) {
        Map<String, KnowledgePageMergeBlockRef> existingMap = existingRefs.stream()
                .collect(Collectors.toMap(KnowledgePageMergeBlockRef::refId, Function.identity()));
        Map<String, KnowledgePageMergeBlockRef> incomingMap = incomingRefs.stream()
                .collect(Collectors.toMap(KnowledgePageMergeBlockRef::refId, Function.identity()));
        List<KitKnowledgePageBlock> blocks = new ArrayList<>();
        List<KitKnowledgePageSourceRef> sourceRefs = new ArrayList<>();
        Set<String> usedRefs = new HashSet<>();
        for (KnowledgePageMergePlan.Item item : plan.items()) {
            if (item == null || !StringUtils.hasText(item.type())) {
                continue;
            }
            switch (item.type()) {
                case "HEADING" -> addStructuralHeading(newVersionId, item.content(), blocks);
                case "EXISTING_BLOCK" -> addReferencedBlock(newVersionId, item.refId(), existingMap, blocks, sourceRefs, usedRefs);
                case "INCOMING_BLOCK" -> addReferencedBlock(newVersionId, item.refId(), incomingMap, blocks, sourceRefs, usedRefs);
                default -> throw new BusinessException(KitErrorCode.E03008);
            }
        }
        appendUnreferencedContent(newVersionId, existingRefs, blocks, sourceRefs, usedRefs);
        appendUnreferencedContent(newVersionId, incomingRefs, blocks, sourceRefs, usedRefs);
        if (CollectionUtils.isEmpty(blocks)) {
            throw new BusinessException(KitErrorCode.E03008);
        }
        resetOrderNo(blocks);
        String title = StringUtils.hasText(plan.title()) ? normalizeTitle(plan.title()) : fallbackTitle;
        return new MergeResult(title, blocks, sourceRefs, renderMarkdown(blocks));
    }

    private void addStructuralHeading(String newVersionId, String content, List<KitKnowledgePageBlock> blocks) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        blocks.add(new KitKnowledgePageBlock()
                .setId(IdWorker.getIdStr())
                .setPageVersionId(newVersionId)
                .setBlockType(KnowledgeBlockType.HEADING)
                .setContent(normalizeHeading(content)));
    }

    private void addReferencedBlock(String newVersionId,
                                    String refId,
                                    Map<String, KnowledgePageMergeBlockRef> refMap,
                                    List<KitKnowledgePageBlock> blocks,
                                    List<KitKnowledgePageSourceRef> sourceRefs,
                                    Set<String> usedRefs) {
        KnowledgePageMergeBlockRef ref = refMap.get(refId);
        if (ref == null || !usedRefs.add(refId)) {
            return;
        }
        copyRef(newVersionId, ref, blocks, sourceRefs);
    }

    private void appendUnreferencedContent(String newVersionId,
                                           List<KnowledgePageMergeBlockRef> refs,
                                           List<KitKnowledgePageBlock> blocks,
                                           List<KitKnowledgePageSourceRef> sourceRefs,
                                           Set<String> usedRefs) {
        for (KnowledgePageMergeBlockRef ref : refs) {
            if (usedRefs.contains(ref.refId()) || ref.block().getBlockType() == KnowledgeBlockType.HEADING) {
                continue;
            }
            usedRefs.add(ref.refId());
            copyRef(newVersionId, ref, blocks, sourceRefs);
        }
    }

    private void copyRef(String newVersionId,
                         KnowledgePageMergeBlockRef ref,
                         List<KitKnowledgePageBlock> blocks,
                         List<KitKnowledgePageSourceRef> sourceRefs) {
        String newBlockId = IdWorker.getIdStr();
        KitKnowledgePageBlock block = new KitKnowledgePageBlock()
                .setId(newBlockId)
                .setPageVersionId(newVersionId)
                .setBlockType(ref.block().getBlockType())
                .setContent(ref.block().getContent());
        blocks.add(block);
        if (ref.sourceRef() != null && ref.block().getBlockType() != KnowledgeBlockType.HEADING) {
            sourceRefs.add(new KitKnowledgePageSourceRef()
                    .setId(IdWorker.getIdStr())
                    .setPageBlockId(newBlockId)
                    .setSourceType(ref.sourceRef().getSourceType())
                    .setSourceDocumentId(ref.sourceRef().getSourceDocumentId())
                    .setSourceLocator(ref.sourceRef().getSourceLocator()));
        }
    }

    private List<KitKnowledgePageBlock> copyRefsToBlocks(String newVersionId, List<KnowledgePageMergeBlockRef> refs) {
        List<KitKnowledgePageBlock> blocks = new ArrayList<>();
        for (KnowledgePageMergeBlockRef ref : refs) {
            blocks.add(new KitKnowledgePageBlock()
                    .setId(IdWorker.getIdStr())
                    .setPageVersionId(newVersionId)
                    .setBlockType(ref.block().getBlockType())
                    .setContent(ref.block().getContent()));
        }
        return blocks;
    }

    private List<KitKnowledgePageSourceRef> copyRefsToSourceRefs(List<KitKnowledgePageBlock> blocks,
                                                                 List<KnowledgePageMergeBlockRef> refs) {
        List<KitKnowledgePageSourceRef> sourceRefs = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            KnowledgePageMergeBlockRef ref = refs.get(i);
            if (ref.sourceRef() == null || ref.block().getBlockType() == KnowledgeBlockType.HEADING) {
                continue;
            }
            sourceRefs.add(new KitKnowledgePageSourceRef()
                    .setId(IdWorker.getIdStr())
                    .setPageBlockId(blocks.get(i).getId())
                    .setSourceType(ref.sourceRef().getSourceType())
                    .setSourceDocumentId(ref.sourceRef().getSourceDocumentId())
                    .setSourceLocator(ref.sourceRef().getSourceLocator()));
        }
        return sourceRefs;
    }

    private void archiveCurrentVersion(String currentVersionId) {
        if (!StringUtils.hasText(currentVersionId)) {
            return;
        }
        pageVersionMapper.update(new KitKnowledgePageVersion()
                        .setVersionStatus(KnowledgePageVersionStatus.ARCHIVED),
                new LambdaUpdateWrapper<KitKnowledgePageVersion>()
                        .eq(KitKnowledgePageVersion::getId, currentVersionId)
                        .eq(KitKnowledgePageVersion::getDeleted, false));
    }

    private void resetOrderNo(List<KitKnowledgePageBlock> blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setOrderNo(i + 1);
        }
    }

    private void validateSingleSource(String sourceDocumentId, KnowledgeBlockBuildResult buildResult) {
        Map<String, KitKnowledgePageSourceRef> refByBlockId = new HashMap<>();
        for (KitKnowledgePageSourceRef ref : buildResult.sourceRefs()) {
            if (!sourceDocumentId.equals(ref.getSourceDocumentId())
                    || refByBlockId.put(ref.getPageBlockId(), ref) != null) {
                throw new BusinessException(KitErrorCode.E03009);
            }
        }
        for (KitKnowledgePageBlock block : buildResult.blocks()) {
            if (block.getBlockType() != KnowledgeBlockType.HEADING && !refByBlockId.containsKey(block.getId())) {
                throw new BusinessException(KitErrorCode.E03009);
            }
        }
    }

    private String renderMarkdown(List<KitKnowledgePageBlock> blocks) {
        return blocks.stream()
                .sorted((left, right) -> Integer.compare(
                        Objects.requireNonNullElse(left.getOrderNo(), 0),
                        Objects.requireNonNullElse(right.getOrderNo(), 0)))
                .map(KitKnowledgePageBlock::getContent)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining("\n\n"));
    }

    private record MergeTarget(String pageId, String lockKey, boolean recheckOnCreate) {
    }

    private record MergeResult(String title,
                               List<KitKnowledgePageBlock> blocks,
                               List<KitKnowledgePageSourceRef> sourceRefs,
                               String markdownContent) {
    }
}
