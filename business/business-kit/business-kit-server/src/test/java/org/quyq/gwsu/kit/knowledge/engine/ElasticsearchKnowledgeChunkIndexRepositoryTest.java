package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeSearchResultVO;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchKnowledgeChunkIndexRepositoryTest {

    @Test
    void ensureIndexCreatesMappingFromSpringDataDocument() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(operations.indexOps(IndexCoordinates.of("test_knowledge_chunk"))).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);
        when(indexOperations.createMapping(KnowledgeChunkDocument.class)).thenReturn(Document.create());
        ElasticsearchKnowledgeChunkIndexRepository repository = repository(operations);

        repository.ensureIndex();

        verify(indexOperations).create();
        verify(indexOperations).putMapping(any(Document.class));
    }

    @Test
    void replacePageVersionDeletesOldChunksThenSavesNewChunks() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(operations.indexOps(IndexCoordinates.of("test_knowledge_chunk"))).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        ElasticsearchKnowledgeChunkIndexRepository repository = repository(operations);
        List<KnowledgeChunkDocument> chunks = List.of(chunk("chunk-1"));

        repository.replacePageVersion("page-1", "version-1", chunks);

        verify(operations).delete(any(DeleteQuery.class), eq(KnowledgeChunkDocument.class), eq(IndexCoordinates.of("test_knowledge_chunk")));
        verify(operations).save(chunks, IndexCoordinates.of("test_knowledge_chunk"));
    }

    @Test
    void searchUsesSpringDataQueryAndMapsHitsToSearchResult() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        ElasticsearchKnowledgeChunkIndexRepository repository = repository(operations);
        SearchHits<KnowledgeChunkDocument> hits = new SearchHitsImpl<>(
                1,
                TotalHitsRelation.EQUAL_TO,
                0.75f,
                Duration.ofMillis(3),
                null,
                null,
                List.of(new SearchHit<>(
                        "test_knowledge_chunk",
                        "chunk-1",
                        null,
                        0.75f,
                        null,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        null,
                        null,
                        Collections.emptyMap(),
                        chunk("chunk-1"))),
                null,
                null,
                null);
        when(operations.search(any(Query.class), eq(KnowledgeChunkDocument.class), eq(IndexCoordinates.of("test_knowledge_chunk"))))
                .thenReturn(hits);

        List<KnowledgeSearchResultVO> results = repository.search("安全", List.of("document-1"), 5);

        assertEquals(1, results.size());
        assertEquals("chunk-1", results.getFirst().getChunkId());
        assertEquals("document-1", results.getFirst().getSourceDocumentId());
        assertEquals(0.75D, results.getFirst().getScore());
    }

    private static ElasticsearchKnowledgeChunkIndexRepository repository(ElasticsearchOperations operations) {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setIndexName("test_knowledge_chunk");
        return new ElasticsearchKnowledgeChunkIndexRepository(operations, properties);
    }

    private static KnowledgeChunkDocument chunk(String chunkId) {
        return new KnowledgeChunkDocument()
                .setChunkId(chunkId)
                .setPageId("page-1")
                .setPageVersionId("version-1")
                .setPageBlockId("block-1")
                .setSourceDocumentId("document-1")
                .setTitle("标题")
                .setHeadingPath("章节")
                .setContent("安全内容")
                .setChunkOrder(1)
                .setContentHash("hash")
                .setStatus("PUBLISHED")
                .setVersion(1)
                .setIndexedAt(Instant.now());
    }
}
