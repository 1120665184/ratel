package org.quyq.gwsu.kit.knowledge.engine.chunk;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgePageVersionStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageSourceRef;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeChunkBuilderTest {

    @Test
    void shouldPrependConsecutiveHeadingsToFirstChunkOnly() {
        KnowledgeProperties properties = new KnowledgeProperties();
        properties.setMaxToken(10);
        KnowledgeChunkBuilder builder = new KnowledgeChunkBuilder(properties);

        KitKnowledgePageVersion pageVersion = new KitKnowledgePageVersion()
                .setId("version-1")
                .setPageId("page-1")
                .setVersionNo(1)
                .setVersionStatus(KnowledgePageVersionStatus.PUBLISHED);

        KitKnowledgePageBlock heading1 = new KitKnowledgePageBlock()
                .setId("block-h1")
                .setOrderNo(1)
                .setBlockType(KnowledgeBlockType.HEADING)
                .setContent("## 第一章");
        KitKnowledgePageBlock heading2 = new KitKnowledgePageBlock()
                .setId("block-h2")
                .setOrderNo(2)
                .setBlockType(KnowledgeBlockType.HEADING)
                .setContent("### 范围");
        KitKnowledgePageBlock paragraph = new KitKnowledgePageBlock()
                .setId("block-p1")
                .setOrderNo(3)
                .setBlockType(KnowledgeBlockType.PARAGRAPH)
                .setContent("第一句内容。第二句内容。");

        KitKnowledgePageSourceRef sourceRef = new KitKnowledgePageSourceRef()
                .setId("ref-1")
                .setPageBlockId("block-p1")
                .setSourceType(KnowledgeSourceType.SOURCE_DOCUMENT)
                .setSourceDocumentId("source-1");

        List<KnowledgeChunkDocument> chunks = builder.build(new KnowledgeChunkBuildRequest(
                "page-1",
                "测试页面",
                pageVersion,
                List.of(heading1, heading2, paragraph),
                List.of(sourceRef)));

        assertEquals(2, chunks.size());
        assertEquals("block-p1", chunks.get(0).getPageBlockId());
        assertEquals("block-p1", chunks.get(1).getPageBlockId());
        assertEquals("## 第一章\n### 范围\n第一句内容。", chunks.get(0).getContent());
        assertEquals("第二句内容。", chunks.get(1).getContent());
        assertEquals("### 范围", chunks.get(0).getHeadingPath());
        assertEquals("### 范围", chunks.get(1).getHeadingPath());
    }
}
