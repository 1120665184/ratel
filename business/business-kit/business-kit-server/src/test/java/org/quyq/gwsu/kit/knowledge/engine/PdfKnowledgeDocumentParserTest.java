package org.quyq.gwsu.kit.knowledge.engine;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.config.properties.KnowledgePdfParseProperties;
import org.springframework.beans.factory.ObjectProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfKnowledgeDocumentParserTest {

    @Test
    void shouldParsePdfByPageAndWarnOnBlankPage() throws Exception {
        Path file = Files.createTempFile("knowledge-pdf-", ".pdf");
        try {
            writePdf(file.toFile());
            ParsedKnowledgeDocument result = parser(file, "guide.pdf", "application/pdf").parse("file-1");

            assertTrue(result.text().contains("Page 1"));
            assertTrue(result.text().contains("First chapter overview"));
            assertTrue(result.text().contains("Page 2"));
            assertTrue(result.parseWarnings().stream().anyMatch(warning -> warning.contains("Page 2")));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldWarnWhenEnhancedModeFallsBackToPdfBox() throws Exception {
        Path file = Files.createTempFile("knowledge-pdf-", ".pdf");
        try {
            writePdf(file.toFile());
            KnowledgePdfParseProperties properties = new KnowledgePdfParseProperties();
            properties.setMode(PdfParseMode.ENHANCED);
            ParsedKnowledgeDocument result = parser(file, "guide.pdf", "application/pdf", properties).parse("file-1");

            assertTrue(result.parseWarnings().stream().anyMatch(warning -> warning.contains("PDFBox")));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private TestPdfKnowledgeDocumentParser parser(Path file, String fileName, String contentType) {
        return parser(file, fileName, contentType, new KnowledgePdfParseProperties());
    }

    private TestPdfKnowledgeDocumentParser parser(Path file,
                                                  String fileName,
                                                  String contentType,
                                                  KnowledgePdfParseProperties properties) {
        ObjectProvider<PdfEnhancedParseStrategy> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.empty());
        return new TestPdfKnowledgeDocumentParser(file.toFile(), new KnowledgeFileMetadata(fileName, contentType), properties, provider);
    }

    private void writePdf(File file) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage firstPage = new PDPage();
            document.addPage(firstPage);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, firstPage)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("First chapter overview");
                contentStream.endText();
            }
            document.addPage(new PDPage());
            document.save(file);
        }
    }

    private static final class TestPdfKnowledgeDocumentParser extends PdfKnowledgeDocumentParser {

        private final File file;

        private final KnowledgeFileMetadata metadata;

        private TestPdfKnowledgeDocumentParser(File file,
                                               KnowledgeFileMetadata metadata,
                                               KnowledgePdfParseProperties properties,
                                               ObjectProvider<PdfEnhancedParseStrategy> provider) {
            super(fileId -> metadata, properties, provider);
            this.file = file;
            this.metadata = metadata;
        }

        @Override
        protected LocalDownloadedKnowledgeFile openLocalFile(String fileId) {
            return LocalDownloadedKnowledgeFile.existing(file, metadata);
        }
    }
}
