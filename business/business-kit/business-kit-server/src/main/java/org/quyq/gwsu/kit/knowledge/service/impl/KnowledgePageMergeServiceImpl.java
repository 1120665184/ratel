package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.GeneratedKnowledgePage;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeBlockBuildResult;
import org.quyq.gwsu.kit.knowledge.engine.KnowledgeBlockFactory;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final TransactionTemplate transactionTemplate;

    @Override
    public String publish(String tenantId, KitKnowledgeSourceDocument sourceDocument, GeneratedKnowledgePage generatedPage) {
        AssertUtils.hasText(tenantId, KitErrorCode.E03005);
        AssertUtils.hasText(sourceDocument.getId(), KitErrorCode.E03009);
        AssertUtils.hasText(generatedPage.markdownContent(), KitErrorCode.E03009);
        String pageId = resolvePageId(sourceDocument);
        return cacheUtils.executeWithLock("knowledge:page:" + pageId, () ->
                transactionTemplate.execute(status -> publishLocked(tenantId, pageId, sourceDocument, generatedPage)));
    }

    private String publishLocked(String tenantId,
                                 String pageId,
                                 KitKnowledgeSourceDocument sourceDocument,
                                 GeneratedKnowledgePage generatedPage) {
        KitKnowledgePage page = ensurePage(tenantId, pageId, generatedPage.title());
        String newVersionId = IdWorker.getIdStr();
        int nextVersionNo = Objects.requireNonNullElse(pageVersionMapper.selectMaxVersionNo(tenantId, page.getId()), 0) + 1;
        KnowledgeBlockBuildResult newSourceBlocks = blockFactory.build(
                newVersionId,
                sourceDocument.getId(),
                generatedPage.markdownContent());
        validateSingleSource(sourceDocument.getId(), newSourceBlocks);

        KitKnowledgePageVersion newVersion = new KitKnowledgePageVersion()
                .setId(newVersionId)
                .setPageId(page.getId())
                .setVersionNo(nextVersionNo)
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED)
                .setMarkdownContent(generatedPage.markdownContent())
                .setPublishedAt(LocalDateTime.now());
        newVersion.setTenantId(tenantId);
        pageVersionMapper.insert(newVersion);

        List<KitKnowledgePageBlock> blocksToInsert = new ArrayList<>();
        List<KitKnowledgePageSourceRef> refsToInsert = new ArrayList<>();
        copyOtherSourceBlocks(tenantId, page.getCurrentVersionId(), newVersionId, sourceDocument.getId(),
                blocksToInsert, refsToInsert);
        blocksToInsert.addAll(newSourceBlocks.blocks());
        refsToInsert.addAll(newSourceBlocks.sourceRefs());
        resetOrderNo(blocksToInsert);
        blocksToInsert.forEach(block -> {
            block.setTenantId(tenantId);
            pageBlockMapper.insert(block);
        });
        refsToInsert.forEach(ref -> {
            ref.setTenantId(tenantId);
            pageSourceRefMapper.insert(ref);
        });

        KitKnowledgePage update = new KitKnowledgePage()
                .setTitle(StringUtils.hasText(generatedPage.title()) ? generatedPage.title() : page.getTitle())
                .setPageStatus(KnowledgePageStatus.PUBLISHED)
                .setCurrentVersionId(newVersionId);
        pageMapper.update(update, new LambdaUpdateWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, page.getId())
                .eq(KitKnowledgePage::getTenantId, tenantId)
                .eq(KitKnowledgePage::getDeleted, false));
        return newVersionId;
    }

    private String resolvePageId(KitKnowledgeSourceDocument sourceDocument) {
        if (StringUtils.hasText(sourceDocument.getTargetPageId())) {
            return sourceDocument.getTargetPageId();
        }
        return IdWorker.getIdStr();
    }

    private KitKnowledgePage ensurePage(String tenantId, String pageId, String title) {
        KitKnowledgePage page = pageMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, pageId)
                .eq(KitKnowledgePage::getTenantId, tenantId)
                .eq(KitKnowledgePage::getDeleted, false));
        if (Objects.nonNull(page)) {
            return page;
        }
        KitKnowledgePage created = new KitKnowledgePage()
                .setId(pageId)
                .setTitle(title)
                .setPageStatus(KnowledgePageStatus.DRAFT);
        created.setTenantId(tenantId);
        pageMapper.insert(created);
        return created;
    }

    private void copyOtherSourceBlocks(String tenantId,
                                       String currentVersionId,
                                       String newVersionId,
                                       String replacingSourceDocumentId,
                                       List<KitKnowledgePageBlock> blocksToInsert,
                                       List<KitKnowledgePageSourceRef> refsToInsert) {
        if (!StringUtils.hasText(currentVersionId)) {
            return;
        }
        List<KitKnowledgePageBlock> currentBlocks = pageBlockMapper.selectByVersionId(tenantId, currentVersionId);
        if (CollectionUtils.isEmpty(currentBlocks)) {
            return;
        }
        List<String> currentBlockIds = currentBlocks.stream()
                .map(KitKnowledgePageBlock::getId)
                .toList();
        Map<String, KitKnowledgePageSourceRef> refByBlockId = pageSourceRefMapper
                .selectByPageBlockIds(tenantId, currentBlockIds)
                .stream()
                .collect(Collectors.toMap(KitKnowledgePageSourceRef::getPageBlockId, Function.identity(), (left, right) -> left));
        for (KitKnowledgePageBlock currentBlock : currentBlocks) {
            KitKnowledgePageSourceRef currentRef = refByBlockId.get(currentBlock.getId());
            if (Objects.isNull(currentRef)
                    || replacingSourceDocumentId.equals(currentRef.getSourceDocumentId())) {
                continue;
            }
            String newBlockId = IdWorker.getIdStr();
            KitKnowledgePageBlock copiedBlock = new KitKnowledgePageBlock()
                    .setId(newBlockId)
                    .setPageVersionId(newVersionId)
                    .setBlockType(currentBlock.getBlockType())
                    .setContent(currentBlock.getContent());
            KitKnowledgePageSourceRef copiedRef = new KitKnowledgePageSourceRef()
                    .setPageBlockId(newBlockId)
                    .setSourceType(currentRef.getSourceType())
                    .setSourceDocumentId(currentRef.getSourceDocumentId())
                    .setSourceLocator(currentRef.getSourceLocator());
            blocksToInsert.add(copiedBlock);
            refsToInsert.add(copiedRef);
        }
    }

    private void resetOrderNo(List<KitKnowledgePageBlock> blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setOrderNo(i + 1);
        }
    }

    private void validateSingleSource(String sourceDocumentId, KnowledgeBlockBuildResult buildResult) {
        if (buildResult.blocks().size() != buildResult.sourceRefs().size()) {
            throw new BusinessException(KitErrorCode.E03009);
        }
        Map<String, KitKnowledgePageSourceRef> refByBlockId = new HashMap<>();
        for (KitKnowledgePageSourceRef ref : buildResult.sourceRefs()) {
            if (!sourceDocumentId.equals(ref.getSourceDocumentId())
                    || refByBlockId.put(ref.getPageBlockId(), ref) != null) {
                throw new BusinessException(KitErrorCode.E03009);
            }
        }
        for (KitKnowledgePageBlock block : buildResult.blocks()) {
            if (!refByBlockId.containsKey(block.getId())) {
                throw new BusinessException(KitErrorCode.E03009);
            }
        }
    }
}
