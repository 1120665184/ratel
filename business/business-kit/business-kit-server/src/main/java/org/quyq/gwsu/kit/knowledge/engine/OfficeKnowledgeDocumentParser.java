package org.quyq.gwsu.kit.knowledge.engine;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Office 文档专用解析器。
 *
 * <p>仅对 docx / pptx / xlsx 使用本地结构化提取；旧版 Office 和 ODF 由 composite 后续回退。</p>
 */
@Component("officeKnowledgeDocumentParser")
public class OfficeKnowledgeDocumentParser extends AbstractLocalKnowledgeDocumentParser {

    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final String PPTX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public OfficeKnowledgeDocumentParser(KnowledgeFileMetadataResolver metadataResolver) {
        super(metadataResolver);
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        return KnowledgeFormatSupport.isDocx(fileName, contentType)
                || KnowledgeFormatSupport.isPptx(fileName, contentType)
                || KnowledgeFormatSupport.isXlsx(fileName, contentType);
    }

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        return parseLocally(fileId);
    }

    @Override
    protected LocalParseResult parseDownloadedFile(File downloadedFile, KnowledgeFileMetadata metadata) throws Exception {
        if (KnowledgeFormatSupport.isDocx(metadata.fileName(), metadata.contentType())) {
            return parseDocx(downloadedFile);
        }
        if (KnowledgeFormatSupport.isPptx(metadata.fileName(), metadata.contentType())) {
            return parsePptx(downloadedFile);
        }
        if (KnowledgeFormatSupport.isXlsx(metadata.fileName(), metadata.contentType())) {
            return parseXlsx(downloadedFile);
        }
        throw new IllegalStateException("当前文件格式不走本地结构化 Office 解析");
    }

    private LocalParseResult parseDocx(File downloadedFile) throws Exception {
        StringBuilder content = new StringBuilder();
        int tableIndex = 1;
        try (FileInputStream inputStream = new FileInputStream(downloadedFile);
             org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream)) {
            for (IBodyElement bodyElement : document.getBodyElements()) {
                if (bodyElement instanceof XWPFParagraph paragraph) {
                    appendParagraph(content, paragraph);
                    continue;
                }
                if (bodyElement instanceof XWPFTable table) {
                    appendTable(content, table, tableIndex++);
                }
            }
        }
        return new LocalParseResult(content.toString().strip(), DOCX_CONTENT_TYPE, List.of());
    }

    private LocalParseResult parsePptx(File downloadedFile) throws Exception {
        StringBuilder content = new StringBuilder();
        try (FileInputStream inputStream = new FileInputStream(downloadedFile);
             XMLSlideShow slideShow = new XMLSlideShow(inputStream)) {
            int slideIndex = 1;
            for (XSLFSlide slide : slideShow.getSlides()) {
                appendBlock(content, "Slide " + slideIndex++);
                String slideTitle = normalizeText(slide.getTitle());
                if (StringUtils.hasText(slideTitle)) {
                    appendBlock(content, "Title: " + slideTitle);
                }
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        appendSlideText(content, textShape, slideTitle);
                    }
                }
            }
        }
        return new LocalParseResult(content.toString().strip(), PPTX_CONTENT_TYPE, List.of());
    }

    private LocalParseResult parseXlsx(File downloadedFile) throws Exception {
        StringBuilder content = new StringBuilder();
        DataFormatter dataFormatter = new DataFormatter();
        try (FileInputStream inputStream = new FileInputStream(downloadedFile);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                appendBlock(content, "Sheet " + (sheetIndex + 1) + ": " + sheet.getSheetName());
                appendBlock(content, "[Table " + (sheetIndex + 1) + "]");
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    short lastCellNum = row.getLastCellNum();
                    if (lastCellNum < 0) {
                        continue;
                    }
                    for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                        String value = normalizeText(dataFormatter.formatCellValue(row.getCell(cellIndex)));
                        if (StringUtils.hasText(value)) {
                            cells.add(columnName(cellIndex) + "=" + value);
                        }
                    }
                    if (cells.isEmpty()) {
                        continue;
                    }
                    appendBlock(content, "Row " + (row.getRowNum() + 1) + ": " + String.join(" | ", cells));
                }
            }
        }
        return new LocalParseResult(content.toString().strip(), XLSX_CONTENT_TYPE, List.of());
    }

    private void appendParagraph(StringBuilder content, XWPFParagraph paragraph) {
        String text = normalizeText(paragraph.getText());
        if (!StringUtils.hasText(text)) {
            return;
        }
        appendBlock(content, (isTitleParagraph(paragraph) ? "Title: " : "Paragraph: ") + text);
    }

    private void appendTable(StringBuilder content, XWPFTable table, int tableIndex) {
        appendBlock(content, "[Table " + tableIndex + "]");
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = row.getTableCells().stream()
                    .map(XWPFTableCell::getText)
                    .map(this::normalizeText)
                    .filter(StringUtils::hasText)
                    .toList();
            if (cells.isEmpty()) {
                continue;
            }
            appendBlock(content, "Table Row: " + String.join(" | ", cells));
        }
    }

    private void appendSlideText(StringBuilder content, XSLFTextShape textShape, String slideTitle) {
        String text = normalizeText(textShape.getText());
        if (!StringUtils.hasText(text) || text.equals(slideTitle)) {
            return;
        }
        appendBlock(content, "Content: " + text);
    }

    private boolean isTitleParagraph(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        if (!StringUtils.hasText(style)) {
            return false;
        }
        String lowerCaseStyle = style.toLowerCase();
        return lowerCaseStyle.contains("title") || lowerCaseStyle.contains("heading");
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', '\n')
                .lines()
                .map(String::strip)
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private String columnName(int columnIndex) {
        StringBuilder columnName = new StringBuilder();
        int currentIndex = columnIndex;
        do {
            columnName.insert(0, (char) ('A' + currentIndex % 26));
            currentIndex = currentIndex / 26 - 1;
        } while (currentIndex >= 0);
        return columnName.toString();
    }

    private void appendBlock(StringBuilder content, String block) {
        if (!StringUtils.hasText(block)) {
            return;
        }
        if (!content.isEmpty()) {
            content.append("\n\n");
        }
        content.append(block);
    }
}
