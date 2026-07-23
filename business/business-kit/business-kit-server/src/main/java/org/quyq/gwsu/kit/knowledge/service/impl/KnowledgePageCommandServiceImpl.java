package org.quyq.gwsu.kit.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgePageSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgePageBlockVO;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuildRequest;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkBuilder;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkDocument;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgeSourceDocumentMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgePageCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识 Page 命令服务实现。
 */
@Service
@RequiredArgsConstructor
public class KnowledgePageCommandServiceImpl implements IKnowledgePageCommandService {

    private final CacheUtils cacheUtils;

    private final KnowledgePageMapper pageMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    private final KnowledgePageBlockMapper pageBlockMapper;

    private final KnowledgePageSourceRefMapper pageSourceRefMapper;

    private final KnowledgeSourceDocumentMapper sourceDocumentMapper;

    private final KnowledgeChunkBuilder chunkBuilder;

    private final KnowledgeChunkEmbeddingService chunkEmbeddingService;

    private final KnowledgeChunkIndexRepository chunkIndexRepository;

    private final TransactionTemplate transactionTemplate;

    @Override
    public String savePage(KnowledgePageSaveDTO dto) {
        AssertUtils.hasText(dto.getTitle(), KitErrorCode.E03009);
        if (CollectionUtils.isEmpty(dto.getBlocks())) {
            throw new BusinessException(KitErrorCode.E03009);
        }
        String pageId = StringUtils.hasText(dto.getId()) ? dto.getId() : IdWorker.getIdStr();
        return cacheUtils.executeWithLock("knowledge:page:" + pageId,
                () -> Objects.requireNonNull(transactionTemplate.execute(status -> savePageInTransaction(pageId, dto))));
    }

    private String savePageInTransaction(String pageId, KnowledgePageSaveDTO dto) {
        List<KnowledgePageBlockVO> normalizedBlocks = normalizeBlocks(dto.getBlocks());
        validateSourceDocuments(normalizedBlocks);

        KitKnowledgePage page = ensurePage(pageId, dto.getTitle());
        validateExistingPageEdit(page.getCurrentVersionId(), normalizedBlocks);
        archiveCurrentVersion(page.getCurrentVersionId());

        String pageVersionId = IdWorker.getIdStr();
        int nextVersionNo = Objects.requireNonNullElse(
                pageVersionMapper.selectMaxVersionNo(pageId), 0) + 1;
        String markdownContent = renderMarkdown(normalizedBlocks);

        KitKnowledgePageVersion version = new KitKnowledgePageVersion()
                .setId(pageVersionId)
                .setPageId(pageId)
                .setVersionNo(nextVersionNo)
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED)
                .setMarkdownContent(markdownContent)
                .setPublishedAt(LocalDateTime.now());
        pageVersionMapper.insert(version);

        List<KitKnowledgePageBlock> blocks = new ArrayList<>(normalizedBlocks.size());
        List<KitKnowledgePageSourceRef> refs = new ArrayList<>(normalizedBlocks.size());
        for (int i = 0; i < normalizedBlocks.size(); i++) {
            KnowledgePageBlockVO blockVO = normalizedBlocks.get(i);
            String blockId = IdWorker.getIdStr();
            KitKnowledgePageBlock block = new KitKnowledgePageBlock()
                    .setId(blockId)
                    .setPageVersionId(pageVersionId)
                    .setOrderNo(i + 1)
                    .setBlockType(blockVO.getBlockType())
                    .setContent(blockVO.getContent());
            blocks.add(block);

            if (blockVO.getBlockType() != KnowledgeBlockType.HEADING) {
                KitKnowledgePageSourceRef ref = new KitKnowledgePageSourceRef()
                        .setId(IdWorker.getIdStr())
                        .setPageBlockId(blockId)
                        .setSourceType(blockVO.getSourceType())
                        .setSourceDocumentId(blockVO.getSourceDocumentId())
                        .setSourceLocator(blockVO.getSourceLocator());
                refs.add(ref);
            }
        }
        blocks.forEach(pageBlockMapper::insert);
        refs.forEach(pageSourceRefMapper::insert);

        KitKnowledgePage update = new KitKnowledgePage()
                .setTitle(dto.getTitle())
                .setPageStatus(KnowledgePageStatus.PUBLISHED)
                .setCurrentVersionId(pageVersionId);
        pageMapper.update(update, new LambdaUpdateWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, pageId)
                .eq(KitKnowledgePage::getDeleted, false));

        List<KnowledgeChunkDocument> chunks = chunkBuilder.build(new KnowledgeChunkBuildRequest(
                pageId,
                dto.getTitle(),
                version,
                blocks,
                refs));
        chunkEmbeddingService.embedChunks(chunks);
        chunkIndexRepository.replacePageVersion(pageId, pageVersionId, chunks);
        return pageId;
    }

    private List<KnowledgePageBlockVO> normalizeBlocks(List<KnowledgePageBlockVO> blocks) {
        List<KnowledgePageBlockVO> normalized = new ArrayList<>();
        for (KnowledgePageBlockVO block : blocks) {
            if (block == null || !StringUtils.hasText(block.getContent())) {
                continue;
            }
            AssertUtils.notNull(block.getBlockType(), KitErrorCode.E03009);
            if (block.getBlockType() != KnowledgeBlockType.HEADING) {
                AssertUtils.notNull(block.getSourceType(), KitErrorCode.E03009);
                AssertUtils.hasText(block.getSourceDocumentId(), KitErrorCode.E03009);
            }
            KnowledgePageBlockVO copy = new KnowledgePageBlockVO()
                    .setId(StringUtils.hasText(block.getId()) ? block.getId().trim() : null)
                    .setBlockType(block.getBlockType())
                    .setContent(block.getContent().trim())
                    .setSourceType(block.getSourceType())
                    .setSourceDocumentId(block.getSourceDocumentId())
                    .setSourceLocator(StringUtils.hasText(block.getSourceLocator()) ? block.getSourceLocator().trim() : null);
            normalized.add(copy);
        }
        if (CollectionUtils.isEmpty(normalized)) {
            throw new BusinessException(KitErrorCode.E03009);
        }
        return List.copyOf(normalized);
    }

    private void validateSourceDocuments(List<KnowledgePageBlockVO> blocks) {
        List<String> sourceDocumentIds = blocks.stream()
                .map(KnowledgePageBlockVO::getSourceDocumentId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(sourceDocumentIds)) {
            return;
        }
        List<KitKnowledgeSourceDocument> sourceDocuments = sourceDocumentMapper.selectList(
                new LambdaQueryWrapper<KitKnowledgeSourceDocument>()
                        .in(KitKnowledgeSourceDocument::getId, sourceDocumentIds)
                        .eq(KitKnowledgeSourceDocument::getDeleted, false));
        Map<String, KitKnowledgeSourceDocument> documentMap = sourceDocuments.stream()
                .collect(Collectors.toMap(KitKnowledgeSourceDocument::getId, Function.identity()));
        for (String sourceDocumentId : sourceDocumentIds) {
            if (!documentMap.containsKey(sourceDocumentId)) {
                throw new BusinessException(KitErrorCode.E03001);
            }
        }
    }

    private void validateExistingPageEdit(String currentVersionId,
                                          List<KnowledgePageBlockVO> blocks) {
        if (!StringUtils.hasText(currentVersionId)) {
            return;
        }
        List<KitKnowledgePageBlock> currentBlocks = pageBlockMapper.selectByVersionId(currentVersionId);
        if (CollectionUtils.isEmpty(currentBlocks)) {
            return;
        }
        Map<String, KitKnowledgePageBlock> currentBlockMap = currentBlocks.stream()
                .collect(Collectors.toMap(KitKnowledgePageBlock::getId, Function.identity()));
        Map<String, KitKnowledgePageSourceRef> currentRefMap = loadCurrentRefMap(currentBlocks);
        Set<String> blockIds = new HashSet<>();
        for (KnowledgePageBlockVO block : blocks) {
            if (!StringUtils.hasText(block.getId())
                    || !blockIds.add(block.getId())
                    || !currentBlockMap.containsKey(block.getId())) {
                throw new BusinessException(KitErrorCode.E03009);
            }
            KitKnowledgePageSourceRef currentRef = currentRefMap.get(block.getId());

            if (currentRef == null
                    || block.getSourceType() != currentRef.getSourceType()
                    || !Objects.equals(block.getSourceDocumentId(), currentRef.getSourceDocumentId())
                    || !Objects.equals(block.getSourceLocator(), currentRef.getSourceLocator())) {
                throw new BusinessException(KitErrorCode.E03009);
            }
        }
    }

    private Map<String, KitKnowledgePageSourceRef> loadCurrentRefMap(List<KitKnowledgePageBlock> currentBlocks) {
        if (CollectionUtils.isEmpty(currentBlocks)) {
            return Map.of();
        }
        Map<String, KitKnowledgePageSourceRef> refMap = new HashMap<>();
        for (KitKnowledgePageSourceRef ref : pageSourceRefMapper.selectByPageBlockIds(
                currentBlocks.stream().map(KitKnowledgePageBlock::getId).toList())) {
            refMap.put(ref.getPageBlockId(), ref);
        }
        return refMap;
    }

    private KitKnowledgePage ensurePage(String pageId, String title) {
        KitKnowledgePage page = pageMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePage>()
                .eq(KitKnowledgePage::getId, pageId)
                .eq(KitKnowledgePage::getDeleted, false));
        if (page != null) {
            return page;
        }
        KitKnowledgePage created = new KitKnowledgePage()
                .setId(pageId)
                .setTitle(title)
                .setPageStatus(KnowledgePageStatus.DRAFT);
        pageMapper.insert(created);
        return created;
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

    private String renderMarkdown(List<KnowledgePageBlockVO> blocks) {
        return blocks.stream()
                .map(KnowledgePageBlockVO::getContent)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining("\n\n"));
    }
}
