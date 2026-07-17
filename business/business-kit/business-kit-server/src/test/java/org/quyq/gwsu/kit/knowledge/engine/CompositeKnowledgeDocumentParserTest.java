package org.quyq.gwsu.kit.knowledge.engine;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.config.properties.KnowledgePdfParseProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Field;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeKnowledgeDocumentParserTest {

    private final KnowledgeFileMetadataResolver metadataResolver = fileId ->
            new KnowledgeFileMetadata("guide.pdf", "application/pdf");

    @Test
    void shouldFallbackToTikaWhenSpecializedParserReturnsEmptyText() {
        KnowledgeDocumentParser pdf = parser("pdf", true, new ParsedKnowledgeDocument("guide.pdf", "application/pdf", "und", " ", List.of()));
        KnowledgeDocumentParser tika = parser("tika", true, ParsedKnowledgeDocument.of("guide.pdf", "可用正文"));
        CompositeKnowledgeDocumentParser parser = new CompositeKnowledgeDocumentParser(pdf, unsupportedParser(), tika, metadataResolver);

        ParsedKnowledgeDocument result = parser.parse("file-1");

        assertEquals("可用正文", result.text());
        assertTrue(result.parseWarnings().stream().anyMatch(warning -> warning.contains("PDF 专用解析结果质量不合格")));
    }

    @Test
    void shouldFallbackToTikaWhenSpecializedParserThrowsException() {
        KnowledgeDocumentParser pdf = parser("pdf", true, new IllegalStateException("损坏文件"));
        KnowledgeDocumentParser tika = parser("tika", true, ParsedKnowledgeDocument.of("guide.pdf", "可用正文"));
        CompositeKnowledgeDocumentParser parser = new CompositeKnowledgeDocumentParser(pdf, unsupportedParser(), tika, metadataResolver);

        ParsedKnowledgeDocument result = parser.parse("file-1");

        assertEquals("可用正文", result.text());
        assertTrue(result.parseWarnings().stream().anyMatch(warning -> warning.contains("PDF 专用解析失败")));
    }

    @Test
    void shouldNotInvokeFallbackWhenSpecializedParserProducesQualifiedText() {
        KnowledgeDocumentParser pdf = parser("pdf", true, ParsedKnowledgeDocument.of("guide.pdf", "这是完整且有效的 PDF 正文。"));
        KnowledgeDocumentParser tika = parser("tika", true, new AssertionError("不应调用 Tika 回退"));
        CompositeKnowledgeDocumentParser parser = new CompositeKnowledgeDocumentParser(pdf, unsupportedParser(), tika, metadataResolver);

        ParsedKnowledgeDocument result = parser.parse("file-1");

        assertEquals("这是完整且有效的 PDF 正文。", result.text());
        assertFalse(result.parseWarnings().stream().anyMatch(warning -> warning.contains("回退")));
    }

    @Test
    void shouldInjectThreeDifferentParsersIntoComposite() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(KnowledgePdfParseProperties.class);
            context.register(TikaKnowledgeDocumentParser.class, PdfKnowledgeDocumentParser.class,
                    OfficeKnowledgeDocumentParser.class, FileUtilsKnowledgeFileMetadataResolver.class,
                    CompositeKnowledgeDocumentParser.class);
            context.refresh();

            CompositeKnowledgeDocumentParser composite = context.getBean(CompositeKnowledgeDocumentParser.class);
            Object pdfParser = field(composite, "pdfParser");
            Object officeParser = field(composite, "officeParser");
            Object tikaParser = field(composite, "tikaParser");

            assertTrue(pdfParser instanceof PdfKnowledgeDocumentParser);
            assertTrue(officeParser instanceof OfficeKnowledgeDocumentParser);
            assertTrue(tikaParser instanceof TikaKnowledgeDocumentParser);
            assertFalse(pdfParser == officeParser || pdfParser == tikaParser || officeParser == tikaParser);
        }
    }

    private KnowledgeDocumentParser unsupportedParser() {
        return parser("office", false, ParsedKnowledgeDocument.of("", ""));
    }

    private KnowledgeDocumentParser parser(String name, boolean supported, Object outcome) {
        return new KnowledgeDocumentParser() {
            @Override
            public boolean supports(String fileName, String contentType) {
                return supported;
            }

            @Override
            public ParsedKnowledgeDocument parse(String fileId) {
                if (outcome instanceof RuntimeException exception) {
                    throw exception;
                }
                if (outcome instanceof Error error) {
                    throw error;
                }
                return (ParsedKnowledgeDocument) outcome;
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
