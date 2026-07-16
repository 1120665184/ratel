package org.quyq.gwsu.kit.knowledge.engine;

import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.model.RerankModelProvider;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索结果重排服务。
 */
@Slf4j
@Component
public class KnowledgeSearchRerankService {

    private static final String CHUNK_ID = "chunk_id";

    public List<KnowledgeSearchResultVO> rerank(String keyword, List<KnowledgeSearchResultVO> results, int size) {
        if (!StringUtils.hasText(keyword) || CollectionUtils.isEmpty(results) || results.size() <= 1) {
            return results;
        }
        try {
            Map<String, KnowledgeSearchResultVO> resultByChunkId = results.stream()
                    .filter(result -> StringUtils.hasText(result.getChunkId()))
                    .collect(LinkedHashMap::new, (map, result) -> map.put(result.getChunkId(), result), LinkedHashMap::putAll);
            List<Document> documents = results.stream()
                    .map(this::toDocument)
                    .toList();
            RerankModel rerankModel = RerankModelProvider.generateModel();
            List<KnowledgeSearchResultVO> reranked = rerankModel.call(new RerankRequest(keyword, documents))
                    .getResults()
                    .stream()
                    .sorted(Comparator.comparing(DocumentWithScore::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(result -> toSearchResult(result, resultByChunkId))
                    .filter(java.util.Objects::nonNull)
                    .limit(size)
                    .toList();
            return reranked.isEmpty() ? results.stream().limit(size).toList() : reranked;
        } catch (RuntimeException ex) {
            log.warn("知识库检索结果重排失败，将使用 ES 原始排序", ex);
            return results.stream().limit(size).toList();
        }
    }

    private Document toDocument(KnowledgeSearchResultVO result) {
        return Document.builder()
                .id(result.getChunkId())
                .text(result.getContent())
                .metadata(CHUNK_ID, result.getChunkId())
                .build();
    }

    private KnowledgeSearchResultVO toSearchResult(
            DocumentWithScore rerankResult,
            Map<String, KnowledgeSearchResultVO> resultByChunkId) {
        Document document = rerankResult.getOutput();
        if (document == null) {
            return null;
        }
        Object chunkId = document.getMetadata().getOrDefault(CHUNK_ID, document.getId());
        KnowledgeSearchResultVO result = resultByChunkId.get(String.valueOf(chunkId));
        if (result == null) {
            return null;
        }
        return result.setScore(rerankResult.getScore());
    }
}
