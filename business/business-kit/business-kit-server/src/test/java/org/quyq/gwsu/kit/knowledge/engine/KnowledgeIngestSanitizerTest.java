package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeIngestSanitizerTest {

    private final KnowledgeIngestSanitizer sanitizer = new KnowledgeIngestSanitizer();

    @Test
    void shouldBeIdempotentAndPreserveFactCharacters() {
        String source = "订单A-2026\r\n\r\n\r\n|   |   |\r\n客户名称\u0000：北京公司\t合同#42";

        SanitizedKnowledgeSource once = sanitizer.sanitize(source);
        SanitizedKnowledgeSource twice = sanitizer.sanitize(once.text());

        assertEquals(once.text(), twice.text());
        assertTrue(once.text().contains("订单A-2026"));
        assertTrue(once.text().contains("北京公司"));
        assertTrue(once.text().contains("合同#42"));
        assertFalse(once.text().contains("\u0000"));
        assertFalse(once.text().contains("|   |   |"));
    }

    @Test
    void shouldOnlyInsertLineBreakAtSafeBoundaryForLongUnbrokenText() {
        String source = "事实编号ABC123，" + "内容".repeat(700);

        SanitizedKnowledgeSource result = sanitizer.sanitize(source);

        assertTrue(result.text().contains("事实编号ABC123，\n"));
        assertEquals(source.replace("，", ""), result.text().replace("，\n", "").replace("\n", ""));
    }

    @Test
    void shouldRemainIdempotentForTrailingLineBreaksAndCodeBlocks() {
        String source = "首行\n\n\n尾行\n```\n代码  A\n\n\n代码  B\n```\n";

        SanitizedKnowledgeSource once = sanitizer.sanitize(source);
        SanitizedKnowledgeSource twice = sanitizer.sanitize(once.text());

        assertEquals("首行\n\n尾行\n```\n代码  A\n\n\n代码  B\n```\n", once.text());
        assertEquals(once.text(), twice.text());
    }

    @Test
    void shouldNotAddTrailingLineBreakWhenRemovingEmptyTableRowAtEndOfFile() {
        SanitizedKnowledgeSource result = sanitizer.sanitize("有效内容\n|  |  |");

        assertEquals("有效内容", result.text());
    }

    @Test
    void shouldPreserveEmptyLookingTableRowsInsideFencedCodeBlock() {
        String source = "正文\n```text\n|  |  |\n```\n|  |  |";

        SanitizedKnowledgeSource result = sanitizer.sanitize(source);

        assertEquals("正文\n```text\n|  |  |\n```", result.text());
    }
}
