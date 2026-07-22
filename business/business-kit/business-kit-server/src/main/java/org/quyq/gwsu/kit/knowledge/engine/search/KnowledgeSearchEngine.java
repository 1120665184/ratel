package org.quyq.gwsu.kit.knowledge.engine.search;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkEmbeddingService;
import org.quyq.gwsu.kit.knowledge.engine.chunk.KnowledgeChunkIndexRepository;
import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeContentRenderService;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageBlockMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageSourceRefMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库检索编排。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeSearchEngine {

    private final IKnowledgeSourceDocumentService sourceDocumentService;

    private final KnowledgeChunkIndexRepository chunkIndexRepository;

    private final KnowledgeChunkEmbeddingService chunkEmbeddingService;

    private final KnowledgeSearchRerankService searchRerankService;

    private final KnowledgeContentRenderService contentRenderService;

    private final KnowledgeProperties properties;

    private final KnowledgePageBlockMapper pageBlockMapper;

    private final KnowledgePageSourceRefMapper pageSourceRefMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    private final KnowledgePageMapper pageMapper;

    private final SecurityUtils securityUtils;

    public List<KnowledgeSearchResultVO> search(KnowledgeSearchDTO dto) {
        List<String> roleCodes = dto.getRoleCodes();
        if (CollectionUtils.isEmpty(roleCodes)) {
            Subject<Visitor> subject = securityUtils.checkSubject();
            roleCodes = subject.getRoles();
        }

        List<String> visibleSourceDocumentIds = sourceDocumentService.listVisibleSourceDocumentIds(roleCodes);
        int size = dto.getSize() == null || dto.getSize() <= 0 ? properties.getSearchSize() : dto.getSize();
        int recallSize = Math.max(size, properties.getHybridRecallSize());
        List<KnowledgeSearchResultVO> results = chunkIndexRepository.search(
                dto.getKeyword(),
                visibleSourceDocumentIds,
                recallSize,
                chunkEmbeddingService.embedQuery(dto.getKeyword()));
        List<KnowledgeSearchResultVO> reranked = searchRerankService.rerank(dto.getKeyword(), results, recallSize);
        return renderSearchResults(reranked, size, dto.getKeyword());
    }

    public List<KnowledgeSearchResultVO> findAdjacentChunk(
            List<String> roleCodes,
            String pageBlockId,
            KnowledgeChunkDirection direction,
            Integer offset) {
        if (!StringUtils.hasText(pageBlockId) || direction == null) {
            throw new BusinessException(KitErrorCode.E03012);
        }
        int limit = offset == null || offset <= 0 ? 1 : offset;

        if(CollectionUtils.isEmpty(roleCodes)) {
            Subject<Visitor> subject = securityUtils.checkSubject();
            roleCodes = subject.getRoles();
        }

        List<String> visibleSourceDocumentIds = sourceDocumentService.listVisibleSourceDocumentIds(roleCodes);
        return findAdjacentVisibleBlocks(pageBlockId, direction, limit, visibleSourceDocumentIds).stream()
                .map(this::renderBlockContent)
                .toList();
    }

    private List<KnowledgeSearchResultVO> renderSearchResults(
            List<KnowledgeSearchResultVO> chunkResults,
            int size,
            String keyword) {
        if (CollectionUtils.isEmpty(chunkResults)) {
            return List.of();
        }
        Map<String, KnowledgeSearchResultVO> chunkByBlockId = new LinkedHashMap<>();
        for (KnowledgeSearchResultVO chunkResult : chunkResults) {
            if (!StringUtils.hasText(chunkResult.getPageBlockId()) || chunkByBlockId.containsKey(chunkResult.getPageBlockId())) {
                continue;
            }
            chunkByBlockId.put(chunkResult.getPageBlockId(), chunkResult);
            if (chunkByBlockId.size() >= size) {
                break;
            }
        }
        if (chunkByBlockId.isEmpty()) {
            return List.of();
        }
        Map<String, KitKnowledgePageBlock> blockById = loadBlockMap(chunkByBlockId.keySet());
        List<KnowledgeSearchResultVO> blockResults = new ArrayList<>(chunkByBlockId.size());
        for (Map.Entry<String, KnowledgeSearchResultVO> entry : chunkByBlockId.entrySet()) {
            KitKnowledgePageBlock block = blockById.get(entry.getKey());
            if (block == null) {
                continue;
            }
            blockResults.add(toBlockSearchResult(entry.getValue(), block, keyword));
        }
        return blockResults;
    }

    private List<KnowledgeSearchResultVO> findAdjacentVisibleBlocks(
            String pageBlockId,
            KnowledgeChunkDirection direction,
            int limit,
            Collection<String> visibleSourceDocumentIds) {
        KitKnowledgePageBlock current = pageBlockMapper.selectById(pageBlockId);
        if (current == null || !StringUtils.hasText(current.getPageVersionId())) {
            return List.of();
        }
        List<KitKnowledgePageBlock> blocks = pageBlockMapper.selectByVersionId(current.getPageVersionId());
        if (CollectionUtils.isEmpty(blocks)) {
            return List.of();
        }
        Map<String, KitKnowledgePageSourceRef> refByBlockId = loadRefMap(blocks);
        if (!isVisibleBlock(current, refByBlockId, visibleSourceDocumentIds)) {
            return List.of();
        }
        int currentIndex = indexOfBlock(blocks, current.getId());
        if (currentIndex < 0) {
            return List.of();
        }
        List<KnowledgeSearchResultVO> results = new ArrayList<>(limit);
        int step = direction == KnowledgeChunkDirection.PREVIOUS ? -1 : 1;
        for (int i = currentIndex + step; i >= 0 && i < blocks.size(); i += step) {
            KitKnowledgePageBlock candidate = blocks.get(i);
            if (!isVisibleBlock(candidate, refByBlockId, visibleSourceDocumentIds)) {
                continue;
            }
            results.add(toAdjacentBlockResult(blocks, i, candidate, refByBlockId.get(candidate.getId())));
            if (results.size() >= limit) {
                break;
            }
        }
        return List.copyOf(results);
    }

    private Map<String, KitKnowledgePageBlock> loadBlockMap(Collection<String> pageBlockIds) {
        if (CollectionUtils.isEmpty(pageBlockIds)) {
            return Map.of();
        }
        Map<String, KitKnowledgePageBlock> blockById = new HashMap<>();
        for (KitKnowledgePageBlock block : pageBlockMapper.selectBatchIds(pageBlockIds)) {
            blockById.put(block.getId(), block);
        }
        return blockById;
    }

    private Map<String, KitKnowledgePageSourceRef> loadRefMap(List<KitKnowledgePageBlock> blocks) {
        if (CollectionUtils.isEmpty(blocks)) {
            return Map.of();
        }
        Map<String, KitKnowledgePageSourceRef> refByBlockId = new HashMap<>();
        pageSourceRefMapper.selectByPageBlockIds(blocks.stream().map(KitKnowledgePageBlock::getId).toList())
                .forEach(ref -> refByBlockId.put(ref.getPageBlockId(), ref));
        return refByBlockId;
    }

    private boolean isVisibleBlock(
            KitKnowledgePageBlock block,
            Map<String, KitKnowledgePageSourceRef> refByBlockId,
            Collection<String> visibleSourceDocumentIds) {
        if (block == null) {
            return false;
        }
        if (block.getBlockType() == KnowledgeBlockType.HEADING) {
            return true;
        }
        KitKnowledgePageSourceRef ref = refByBlockId.get(block.getId());
        return ref != null
                && StringUtils.hasText(ref.getSourceDocumentId())
                && visibleSourceDocumentIds.contains(ref.getSourceDocumentId());
    }

    private int indexOfBlock(List<KitKnowledgePageBlock> blocks, String blockId) {
        for (int i = 0; i < blocks.size(); i++) {
            if (Objects.equals(blocks.get(i).getId(), blockId)) {
                return i;
            }
        }
        return -1;
    }

    private KnowledgeSearchResultVO toBlockSearchResult(
            KnowledgeSearchResultVO chunkResult,
            KitKnowledgePageBlock block,
            String keyword) {
        String renderedBlockContent = contentRenderService.render(block.getContent());
        return new KnowledgeSearchResultVO()
                .setChunkId(chunkResult.getChunkId())
                .setPageId(chunkResult.getPageId())
                .setPageVersionId(chunkResult.getPageVersionId())
                .setPageBlockId(block.getId())
                .setBlockType(block.getBlockType())
                .setSourceDocumentId(chunkResult.getSourceDocumentId())
                .setTitle(chunkResult.getTitle())
                .setHeadingPath(chunkResult.getHeadingPath())
                .setContent(highlightKeyword(renderedBlockContent, keyword))
                .setBlockOrder(block.getOrderNo())
                .setChunkOrder(chunkResult.getChunkOrder())
                .setScore(chunkResult.getScore());
    }

    private KnowledgeSearchResultVO toAdjacentBlockResult(
            List<KitKnowledgePageBlock> blocks,
            int blockIndex,
            KitKnowledgePageBlock block,
            KitKnowledgePageSourceRef ref) {
        KitKnowledgePageVersion version = pageVersionMapper.selectById(block.getPageVersionId());
        KitKnowledgePage page = version == null ? null : pageMapper.selectById(version.getPageId());
        return new KnowledgeSearchResultVO()
                .setPageId(version == null ? null : version.getPageId())
                .setPageVersionId(block.getPageVersionId())
                .setPageBlockId(block.getId())
                .setBlockType(block.getBlockType())
                .setSourceDocumentId(ref == null ? null : ref.getSourceDocumentId())
                .setTitle(page == null ? null : page.getTitle())
                .setHeadingPath(resolveHeadingPath(blocks, blockIndex))
                .setContent(block.getContent())
                .setBlockOrder(block.getOrderNo());
    }

    private String resolveHeadingPath(List<KitKnowledgePageBlock> blocks, int blockIndex) {
        String headingPath = "";
        for (int i = 0; i <= blockIndex && i < blocks.size(); i++) {
            KitKnowledgePageBlock current = blocks.get(i);
            if (current.getBlockType() == KnowledgeBlockType.HEADING) {
                headingPath = current.getContent();
            }
        }
        return headingPath;
    }

    private String highlightKeyword(String content, String keyword) {
        if (!StringUtils.hasText(content) || !StringUtils.hasText(keyword)) {
            return content;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "<mark>" + Matcher.quoteReplacement(matcher.group()) + "</mark>");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private KnowledgeSearchResultVO renderBlockContent(KnowledgeSearchResultVO result) {
        return result.setContent(contentRenderService.render(result.getContent()));
    }
}
