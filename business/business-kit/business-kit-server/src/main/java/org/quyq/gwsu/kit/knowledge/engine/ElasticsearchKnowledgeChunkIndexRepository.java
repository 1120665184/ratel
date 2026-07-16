package org.quyq.gwsu.kit.knowledge.engine;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 知识 Chunk 索引仓储。
 */
@Repository
@RequiredArgsConstructor
public class ElasticsearchKnowledgeChunkIndexRepository implements KnowledgeChunkIndexRepository {

    private final ElasticsearchClient elasticsearchClient;

    private final KnowledgeProperties properties;

    @Override
    public void ensureIndex() {
        try {
            String indexName = properties.getIndexName();
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(request -> request.index(indexName)))
                    .value();
            if (exists) {
                return;
            }
            elasticsearchClient.indices().create(CreateIndexRequest.of(request -> request
                    .index(indexName)
                    .mappings(defaultMapping())));
        } catch (IOException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    @Override
    public void replacePageVersion(String pageId, String pageVersionId, List<KnowledgeChunkDocument> chunks) {
        ensureIndex();
        try {
            elasticsearchClient.deleteByQuery(DeleteByQueryRequest.of(request -> request
                    .index(properties.getIndexName())
                    .query(query -> query.bool(bool -> bool
                            .filter(filter -> filter.term(term -> term.field("page_id").value(pageId)))
                            .filter(filter -> filter.term(term -> term.field("page_version_id").value(pageVersionId)))))));
            if (CollectionUtils.isEmpty(chunks)) {
                return;
            }
            BulkRequest.Builder bulk = new BulkRequest.Builder().index(properties.getIndexName());
            for (KnowledgeChunkDocument chunk : chunks) {
                bulk.operations(operation -> operation
                        .index(index -> index
                                .id(chunk.chunkId())
                                .document(toEsDocument(chunk))));
            }
            elasticsearchClient.bulk(bulk.build());
        } catch (IOException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    @Override
    public List<KnowledgeSearchResultVO> search(String keyword, Collection<String> visibleSourceDocumentIds, int size) {
        if (CollectionUtils.isEmpty(visibleSourceDocumentIds)) {
            return List.of();
        }
        try {
            SearchRequest request = SearchRequest.of(search -> search
                    .index(properties.getIndexName())
                    .size(size)
                    .query(query -> query.bool(bool -> bool
                            .must(must -> must.match(match -> match.field("content").query(keyword)))
                            .filter(filter -> filter.terms(terms -> terms
                                    .field("source_document_id")
                                    .terms(values -> values.value(visibleSourceDocumentIds.stream()
                                            .map(FieldValue::of)
                                            .toList())))))));
            SearchResponse<Map> response =
                    elasticsearchClient.search(request, Map.class);
            return response.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(hit -> toSearchResult(hit.source(), hit.score()))
                    .toList();
        } catch (IOException ex) {
            throw new BusinessException(KitErrorCode.E03011, ex);
        }
    }

    private TypeMapping defaultMapping() {
        return TypeMapping.of(mapping -> mapping
                .properties("chunk_id", property -> property.keyword(keyword -> keyword))
                .properties("page_id", property -> property.keyword(keyword -> keyword))
                .properties("page_version_id", property -> property.keyword(keyword -> keyword))
                .properties("page_block_id", property -> property.keyword(keyword -> keyword))
                .properties("source_document_id", property -> property.keyword(keyword -> keyword))
                .properties("title", property -> property.text(text -> text))
                .properties("heading_path", property -> property.text(text -> text))
                .properties("content", property -> property.text(text -> text))
                .properties("chunk_order", property -> property.integer(integer -> integer))
                .properties("content_hash", property -> property.keyword(keyword -> keyword))
                .properties("status", property -> property.keyword(keyword -> keyword))
                .properties("version", property -> property.integer(integer -> integer))
                .properties("indexed_at", property -> property.date(date -> date))
                .properties("embedding", property -> property.denseVector(vector -> vector))
                .properties("embedding_model", property -> property.keyword(keyword -> keyword)));
    }

    private Map<String, Object> toEsDocument(KnowledgeChunkDocument chunk) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("chunk_id", chunk.chunkId());
        document.put("page_id", chunk.pageId());
        document.put("page_version_id", chunk.pageVersionId());
        document.put("page_block_id", chunk.pageBlockId());
        document.put("source_document_id", chunk.sourceDocumentId());
        document.put("title", chunk.title());
        document.put("heading_path", chunk.headingPath());
        document.put("content", chunk.content());
        document.put("chunk_order", chunk.chunkOrder());
        document.put("content_hash", chunk.contentHash());
        document.put("status", chunk.status());
        document.put("version", chunk.version());
        document.put("indexed_at", chunk.indexedAt());
        document.put("embedding", chunk.embedding());
        document.put("embedding_model", chunk.embeddingModel());
        return document;
    }

    private KnowledgeSearchResultVO toSearchResult(Map source, Double score) {
        return new KnowledgeSearchResultVO()
                .setChunkId(toString(source.get("chunk_id")))
                .setPageId(toString(source.get("page_id")))
                .setPageVersionId(toString(source.get("page_version_id")))
                .setPageBlockId(toString(source.get("page_block_id")))
                .setSourceDocumentId(toString(source.get("source_document_id")))
                .setTitle(toString(source.get("title")))
                .setHeadingPath(toString(source.get("heading_path")))
                .setContent(toString(source.get("content")))
                .setChunkOrder(toInteger(source.get("chunk_order")))
                .setScore(score);
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer toInteger(Object value) {
        return switch (value) {
            case null -> null;
            case Integer integer -> integer;
            case Number number -> number.intValue();
            default -> Integer.valueOf(value.toString());
        };
    }
}
