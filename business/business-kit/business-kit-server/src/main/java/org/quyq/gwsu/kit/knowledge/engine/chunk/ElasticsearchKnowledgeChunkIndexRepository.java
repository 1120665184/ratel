package org.quyq.gwsu.kit.knowledge.engine.chunk;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.config.properties.ModelEmbeddingConfigDTO;
import org.quyq.gwsu.common.ai.model.EmbeddingModelProvider;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Elasticsearch 知识 Chunk 索引仓储。
 */
@Repository
@RequiredArgsConstructor
public class ElasticsearchKnowledgeChunkIndexRepository implements KnowledgeChunkIndexRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    private final KnowledgeProperties properties;

    @Override
    public void ensureIndex() {
        try {
            IndexOperations indexOperations = elasticsearchOperations.indexOps(indexCoordinates());
            if (indexOperations.exists()) {
                return;
            }
            indexOperations.create();
            indexOperations.putMapping(buildIndexMapping());
        } catch (RuntimeException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    @Override
    public void replacePageVersion(String pageId, String pageVersionId, List<KnowledgeChunkDocument> chunks) {
        ensureIndex();
        try {
            elasticsearchOperations.delete(DeleteQuery.builder(pageQuery(pageId)).build(),
                    KnowledgeChunkDocument.class,
                    indexCoordinates());
            if (CollectionUtils.isEmpty(chunks)) {
                return;
            }
            elasticsearchOperations.save(chunks, indexCoordinates());
        } catch (RuntimeException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    @Override
    public void deleteBySourceDocumentId(String sourceDocumentId) {
        if (!StringUtils.hasText(sourceDocumentId)) {
            return;
        }
        ensureIndex();
        try {
            elasticsearchOperations.delete(DeleteQuery.builder(sourceDocumentQuery(sourceDocumentId)).build(),
                    KnowledgeChunkDocument.class,
                    indexCoordinates());
        } catch (RuntimeException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    @Override
    public List<KnowledgeSearchResultVO> search(
            String keyword,
            Collection<String> visibleSourceDocumentIds,
            int size,
            Optional<float[]> queryEmbedding) {
        if (CollectionUtils.isEmpty(visibleSourceDocumentIds)) {
            return List.of();
        }
        ensureIndex();
        try {
            return elasticsearchOperations.search(searchQuery(keyword, visibleSourceDocumentIds, size, queryEmbedding),
                            KnowledgeChunkDocument.class,
                            indexCoordinates())
                    .stream()
                    .map(this::toSearchResult)
                    .toList();
        } catch (RuntimeException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    @Override
    public Optional<KnowledgeSearchResultVO> findAdjacentChunk(
            String chunkId,
            KnowledgeChunkDirection direction,
            Collection<String> visibleSourceDocumentIds) {
        if (CollectionUtils.isEmpty(visibleSourceDocumentIds)) {
            return Optional.empty();
        }
        ensureIndex();
        try {
            KnowledgeChunkDocument current = elasticsearchOperations.get(
                    chunkId,
                    KnowledgeChunkDocument.class,
                    indexCoordinates());
            if (current == null || !visibleSourceDocumentIds.contains(current.getSourceDocumentId())) {
                return Optional.empty();
            }
            return elasticsearchOperations.search(adjacentQuery(current, direction, visibleSourceDocumentIds),
                            KnowledgeChunkDocument.class,
                            indexCoordinates())
                    .stream()
                    .findFirst()
                    .map(this::toSearchResult);
        } catch (RuntimeException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    private Query pageQuery(String pageId) {
        return CriteriaQuery.builder(Criteria.where("page_id").is(pageId)).build();
    }

    private Query sourceDocumentQuery(String sourceDocumentId) {
        return CriteriaQuery.builder(Criteria.where("source_document_id").is(sourceDocumentId)).build();
    }

    private Query searchQuery(
            String keyword,
            Collection<String> visibleSourceDocumentIds,
            int size,
            Optional<float[]> queryEmbedding) {
        if (queryEmbedding.isPresent()) {
            return hybridSearchQuery(keyword, visibleSourceDocumentIds, size, queryEmbedding.get());
        }
        Criteria criteria = Criteria.where("content").matches(keyword)
                .and("source_document_id").in(visibleSourceDocumentIds);
        return CriteriaQuery.builder(criteria)
                .withMaxResults(size)
                .build();
    }

    private Query hybridSearchQuery(
            String keyword,
            Collection<String> visibleSourceDocumentIds,
            int size,
            float[] queryEmbedding) {
        co.elastic.clients.elasticsearch._types.query_dsl.Query sourceFilter = sourceDocumentTermsQuery(visibleSourceDocumentIds);
        return NativeQuery.builder()
                .withQuery(query -> query.bool(bool -> bool
                        .must(must -> must.match(match -> match.field("content").query(keyword)))
                        .filter(sourceFilter)))
                .withKnnSearches(KnnSearch.of(knn -> knn
                        .field("embedding")
                        .queryVector(toFloatList(queryEmbedding))
                        .k(size)
                        .numCandidates(Math.max(size * 5, 20))
                        .filter(sourceFilter)))
                .withMaxResults(size)
                .build();
    }

    private co.elastic.clients.elasticsearch._types.query_dsl.Query sourceDocumentTermsQuery(
            Collection<String> visibleSourceDocumentIds) {
        return co.elastic.clients.elasticsearch._types.query_dsl.Query.of(query -> query.terms(terms -> terms
                .field("source_document_id")
                .terms(values -> values.value(visibleSourceDocumentIds.stream()
                        .map(FieldValue::of)
                        .toList()))));
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> values = new java.util.ArrayList<>(vector.length);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }

    private Query adjacentQuery(
            KnowledgeChunkDocument current,
            KnowledgeChunkDirection direction,
            Collection<String> visibleSourceDocumentIds) {
        Criteria criteria = Criteria.where("page_version_id").is(current.getPageVersionId())
                .and("source_document_id").in(visibleSourceDocumentIds);
        Sort sort;
        if (direction == KnowledgeChunkDirection.PREVIOUS) {
            criteria = criteria.and("chunk_order").lessThan(current.getChunkOrder());
            sort = Sort.by(Sort.Direction.DESC, "chunk_order");
        } else {
            criteria = criteria.and("chunk_order").greaterThan(current.getChunkOrder());
            sort = Sort.by(Sort.Direction.ASC, "chunk_order");
        }
        return CriteriaQuery.builder(criteria)
                .withSort(sort)
                .withMaxResults(1)
                .build();
    }

    private KnowledgeSearchResultVO toSearchResult(SearchHit<KnowledgeChunkDocument> searchHit) {
        KnowledgeChunkDocument chunk = searchHit.getContent();
        return new KnowledgeSearchResultVO()
                .setChunkId(chunk.getChunkId())
                .setPageId(chunk.getPageId())
                .setPageVersionId(chunk.getPageVersionId())
                .setPageBlockId(chunk.getPageBlockId())
                .setSourceDocumentId(chunk.getSourceDocumentId())
                .setTitle(chunk.getTitle())
                .setHeadingPath(chunk.getHeadingPath())
                .setContent(chunk.getContent())
                .setChunkOrder(chunk.getChunkOrder())
                .setScore((double) searchHit.getScore());
    }

    private IndexCoordinates indexCoordinates() {
        return IndexCoordinates.of(properties.getIndexName());
    }

    private Document buildIndexMapping() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("chunk_id", keywordField());
        properties.put("page_id", keywordField());
        properties.put("page_version_id", keywordField());
        properties.put("page_block_id", keywordField());
        properties.put("source_document_id", keywordField());
        properties.put("title", textField());
        properties.put("heading_path", textField());
        properties.put("content", textField());
        properties.put("chunk_order", Map.of("type", "integer"));
        properties.put("content_hash", keywordField());
        properties.put("status", keywordField());
        properties.put("version", Map.of("type", "integer"));
        properties.put("indexed_at", Map.of("type", "date"));
        properties.put("embedding", denseVectorField(resolveEmbeddingDimensions()));
        properties.put("embedding_model", keywordField());
        return Document.from(Map.of("properties", properties));
    }

    private Map<String, Object> keywordField() {
        return Map.of("type", "keyword");
    }

    private Map<String, Object> textField() {
        return Map.of("type", "text");
    }

    private Map<String, Object> denseVectorField(int dims) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", "dense_vector");
        field.put("dims", dims);
        field.put("similarity", "cosine");
        return field;
    }

    private int resolveEmbeddingDimensions() {
        ModelEmbeddingConfigDTO config = ConfigInfoUtils.getByObject(
                EmbeddingModelProvider.MODEL_EMBEDDING_CONFIG,
                ModelEmbeddingConfigDTO.class);
        if (config == null || !StringUtils.hasText(config.getProvider())) {
            return 1024;
        }
        Integer dimensions = switch (config.getProvider().trim().toLowerCase()) {
            case "dashscope" -> config.getDashscope() == null ? null : config.getDashscope().getDimensions();
            case "openai" -> config.getOpenai() == null ? null : config.getOpenai().getDimensions();
            case "ollama" -> config.getOllama() == null ? null : config.getOllama().getDimensions();
            case "zhipuai" -> config.getZhipuai() == null ? null : config.getZhipuai().getDimensions();
            default -> null;
        };
        if (dimensions == null || dimensions <= 0) {
            return 1024;
        }
        return dimensions;
    }
}
