package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeSourceType;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageBlock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeBlockFactoryTest {

    private final KnowledgeBlockFactory factory = new KnowledgeBlockFactory();

    @Test
    void markdownBuildsOrderedBlocksAndOneSourceRefPerBlock() {
        String markdown = """
                # 标题
                
                这是一个段落。
                
                - 第一项
                - 第二项
                
                | 列A | 列B |
                | --- | --- |
                | A1 | B1 |
                """;

        KnowledgeBlockBuildResult result = factory.build("version-1", "document-1", markdown);

        List<KnowledgeBlockType> blockTypes = result.blocks().stream()
                .map(KitKnowledgePageBlock::getBlockType)
                .toList();
        assertEquals(List.of(
                KnowledgeBlockType.HEADING,
                KnowledgeBlockType.PARAGRAPH,
                KnowledgeBlockType.LIST,
                KnowledgeBlockType.TABLE), blockTypes);
        assertEquals(result.blocks().size(), result.sourceRefs().size());
        assertTrue(result.sourceRefs().stream()
                .allMatch(ref -> "document-1".equals(ref.getSourceDocumentId())));
        assertTrue(result.sourceRefs().stream()
                .allMatch(ref -> ref.getSourceType() == KnowledgeSourceType.SOURCE_DOCUMENT));
        assertFalse(result.sourceRefs().stream()
                .anyMatch(ref -> ref.getPageBlockId() == null || ref.getPageBlockId().isBlank()));
    }

    @Test
    void fencedCodeKeepsOpeningAndClosingFenceInOneBlock() {
        String markdown = """
                ```java
                class Demo {
                }
                ```
                
                后续段落
                """;

        KnowledgeBlockBuildResult result = factory.build("version-1", "document-1", markdown);

        assertEquals(2, result.blocks().size());
        assertEquals(KnowledgeBlockType.CODE, result.blocks().getFirst().getBlockType());
        assertTrue(result.blocks().getFirst().getContent().endsWith("```"));
        assertEquals(KnowledgeBlockType.PARAGRAPH, result.blocks().get(1).getBlockType());
        assertEquals("line:1-4", result.sourceRefs().getFirst().getSourceLocator());
    }
}
