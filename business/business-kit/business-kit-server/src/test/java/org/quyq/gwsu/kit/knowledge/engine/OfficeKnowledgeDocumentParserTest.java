package org.quyq.gwsu.kit.knowledge.engine;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficeKnowledgeDocumentParserTest {

    @Test
    void shouldParseDocxTitlesParagraphsAndTableRows() throws Exception {
        Path file = Files.createTempFile("knowledge-office-", ".docx");
        try {
            writeDocx(file.toFile());
            ParsedKnowledgeDocument result = parser(file, "guide.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document").parse("file-1");

            assertTrue(result.text().contains("Title: 项目说明"));
            assertTrue(result.text().contains("Paragraph: 上线步骤"));
            assertTrue(result.text().contains("[Table 1]"));
            assertTrue(result.text().contains("Table Row: 姓名 | 数量"));
            assertTrue(result.text().contains("Table Row: 张三 | 7"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldParsePptxSlideBoundariesTitleAndContent() throws Exception {
        Path file = Files.createTempFile("knowledge-office-", ".pptx");
        try {
            writePptx(file.toFile());
            ParsedKnowledgeDocument result = parser(file, "deck.pptx",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation").parse("file-1");

            assertTrue(result.text().contains("Slide 1"));
            assertTrue(result.text().contains("Title: 发布计划"));
            assertTrue(result.text().contains("Content: 阶段一"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldParseXlsxSheetBoundariesAndRowColumnText() throws Exception {
        Path file = Files.createTempFile("knowledge-office-", ".xlsx");
        try {
            writeXlsx(file.toFile());
            ParsedKnowledgeDocument result = parser(file, "report.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").parse("file-1");

            assertTrue(result.text().contains("Sheet 1: 月报"));
            assertTrue(result.text().contains("[Table 1]"));
            assertTrue(result.text().contains("Row 1: A=城市 | B=数量"));
            assertTrue(result.text().contains("Row 2: A=北京 | B=12"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldOnlySupportOoxmlFormats() {
        OfficeKnowledgeDocumentParser parser = new OfficeKnowledgeDocumentParser(fileId -> new KnowledgeFileMetadata("guide.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

        assertTrue(parser.supports("guide.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertTrue(parser.supports("deck.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        assertTrue(parser.supports("report.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertFalse(parser.supports("legacy.doc", "application/msword"));
        assertFalse(parser.supports("legacy.xls", "application/vnd.ms-excel"));
        assertFalse(parser.supports("legacy.ppt", "application/vnd.ms-powerpoint"));
        assertFalse(parser.supports("guide.odt", "application/vnd.oasis.opendocument.text"));
    }

    private TestOfficeKnowledgeDocumentParser parser(Path file, String fileName, String contentType) {
        return new TestOfficeKnowledgeDocumentParser(file.toFile(), new KnowledgeFileMetadata(fileName, contentType));
    }

    private void writeDocx(File file) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream outputStream = new FileOutputStream(file)) {
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Title");
            title.createRun().setText("项目说明");

            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("上线步骤");

            var table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("姓名");
            table.getRow(0).getCell(1).setText("数量");
            table.getRow(1).getCell(0).setText("张三");
            table.getRow(1).getCell(1).setText("7");
            document.write(outputStream);
        }
    }

    private void writePptx(File file) throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow();
             FileOutputStream outputStream = new FileOutputStream(file)) {
            XSLFSlide slide = slideShow.createSlide(slideShow.getSlideMasters().getFirst().getLayout(SlideLayout.TITLE_AND_CONTENT));
            XSLFTextShape title = slide.getPlaceholder(0);
            title.clearText();
            title.setText("发布计划");
            XSLFTextShape body = slide.getPlaceholder(1);
            body.clearText();
            body.addNewTextParagraph().addNewTextRun().setText("阶段一");
            slideShow.write(outputStream);
        }
    }

    private void writeXlsx(File file) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream outputStream = new FileOutputStream(file)) {
            var sheet = workbook.createSheet("月报");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("城市");
            header.createCell(1).setCellValue("数量");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("北京");
            row.createCell(1).setCellValue(12);
            workbook.write(outputStream);
        }
    }

    private static final class TestOfficeKnowledgeDocumentParser extends OfficeKnowledgeDocumentParser {

        private final File file;

        private final KnowledgeFileMetadata metadata;

        private TestOfficeKnowledgeDocumentParser(File file, KnowledgeFileMetadata metadata) {
            super(fileId -> metadata);
            this.file = file;
            this.metadata = metadata;
        }

        @Override
        protected LocalDownloadedKnowledgeFile openLocalFile(String fileId) {
            return LocalDownloadedKnowledgeFile.existing(file, metadata);
        }
    }
}
