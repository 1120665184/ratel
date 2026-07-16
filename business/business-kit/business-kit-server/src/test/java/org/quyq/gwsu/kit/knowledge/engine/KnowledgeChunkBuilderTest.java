package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeChunkBuilderTest {

    @Test
    void longBlockSplitsOnNaturalBoundaryAndKeepsSameSource() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setMaxToken(10);
        KnowledgeChunkBuilder builder = new KnowledgeChunkBuilder(properties);

        KitKnowledgePageBlock block = block("block-1", 1, "第一句很长。第二句也很长。第三句。");
        KitKnowledgePageSourceRef ref = ref("block-1", "document-a");

        List<KnowledgeChunkDocument> chunks = builder.build(new KnowledgeChunkBuildRequest(
                "page-1",
                "标题",
                version(),
                List.of(block),
                List.of(ref)));

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> "block-1".equals(chunk.pageBlockId())));
        assertTrue(chunks.stream().allMatch(chunk -> "document-a".equals(chunk.sourceDocumentId())));
        assertTrue(chunks.getFirst().content().endsWith("。"));
    }

    @Test
    void blocksFromDifferentSourcesNeverMergeIntoOneChunk() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setMaxToken(100);
        KnowledgeChunkBuilder builder = new KnowledgeChunkBuilder(properties);

        List<KnowledgeChunkDocument> chunks = builder.build(new KnowledgeChunkBuildRequest(
                "page-1",
                "标题",
                version(),
                List.of(
                        block("block-1", 1, "来源A短段落"),
                        block("block-2", 2, "来源B短段落")),
                List.of(
                        ref("block-1", "document-a"),
                        ref("block-2", "document-b"))));

        assertEquals(2, chunks.size());
        assertEquals(List.of("document-a", "document-b"), chunks.stream()
                .map(KnowledgeChunkDocument::sourceDocumentId)
                .toList());
    }

    private static KitKnowledgePageVersion version() {
        return new KitKnowledgePageVersion()
                .setId("version-1")
                .setPageId("page-1")
                .setVersionNo(1);
    }

    private static KitKnowledgePageBlock block(String id, int orderNo, String content) {
        return new KitKnowledgePageBlock()
                .setId(id)
                .setPageVersionId("version-1")
                .setOrderNo(orderNo)
                .setBlockType(KnowledgeBlockType.PARAGRAPH)
                .setContent(content);
    }

    private static KitKnowledgePageSourceRef ref(String blockId, String sourceDocumentId) {
        return new KitKnowledgePageSourceRef()
                .setPageBlockId(blockId)
                .setSourceType(KnowledgeSourceType.SOURCE_DOCUMENT)
                .setSourceDocumentId(sourceDocumentId)
                .setSourceLocator("line:1-1");
    }
}
