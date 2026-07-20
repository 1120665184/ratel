package org.quyq.gwsu.kit.knowledge.engine;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeSearchDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.knowledge.service.IKnowledgeSourceDocumentService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    private final KnowledgeProperties properties;

    public List<KnowledgeSearchResultVO> search(KnowledgeSearchDTO dto) {
        List<String> visibleSourceDocumentIds = sourceDocumentService.listVisibleSourceDocumentIds(dto.getRoleCodes());
        int size = dto.getSize() == null || dto.getSize() <= 0 ? properties.getSearchSize() : dto.getSize();
        int recallSize = Math.max(size, properties.getHybridRecallSize());
        List<KnowledgeSearchResultVO> results = chunkIndexRepository.search(
                dto.getKeyword(),
                visibleSourceDocumentIds,
                recallSize,
                chunkEmbeddingService.embedQuery(dto.getKeyword()));
        return searchRerankService.rerank(dto.getKeyword(), results, size);
    }

    public Optional<KnowledgeSearchResultVO> findAdjacentChunk(
            Collection<String> roleCodes,
            String chunkId,
            KnowledgeChunkDirection direction) {
        if (!StringUtils.hasText(chunkId) || direction == null) {
            throw new BusinessException(KitErrorCode.E03005);
        }
        List<String> visibleSourceDocumentIds = sourceDocumentService.listVisibleSourceDocumentIds(roleCodes);
        return chunkIndexRepository.findAdjacentChunk(chunkId, direction, visibleSourceDocumentIds);
    }
}
