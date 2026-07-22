package org.quyq.gwsu.kit.knowledge.engine.ingest;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeImageMarkerSupport;
import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeImageOcrResult;
import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeImageOcrService;
import org.quyq.gwsu.kit.knowledge.engine.image.KnowledgeImageUploadService;
import org.quyq.gwsu.kit.knowledge.engine.support.KnowledgeFileMetadata;
import org.quyq.gwsu.kit.knowledge.engine.support.KnowledgeFileMetadataResolver;
import org.quyq.gwsu.kit.knowledge.engine.support.KnowledgeFormatSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Office 文档专用解析器。
 *
 * <p>对 doc / docx / pptx / xlsx 使用本地结构化提取；其他旧版 Office 和 ODF 由 composite 后续回退。</p>
 */
@Component("officeKnowledgeDocumentParser")
public class OfficeKnowledgeDocumentParser extends AbstractLocalKnowledgeDocumentParser {

    private static final String DOC_CONTENT_TYPE = "application/msword";

    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final String PPTX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final KnowledgeImageUploadService knowledgeImageUploadService;

    private final KnowledgeImageOcrService knowledgeImageOcrService;

    public OfficeKnowledgeDocumentParser(KnowledgeFileMetadataResolver metadataResolver,
                                         KnowledgeImageUploadService knowledgeImageUploadService,
                                         KnowledgeImageOcrService knowledgeImageOcrService) {
        super(metadataResolver);
        this.knowledgeImageUploadService = knowledgeImageUploadService;
        this.knowledgeImageOcrService = knowledgeImageOcrService;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        return KnowledgeFormatSupport.isDoc(fileName, contentType)
                || KnowledgeFormatSupport.isDocx(fileName, contentType)
                || KnowledgeFormatSupport.isPptx(fileName, contentType)
                || KnowledgeFormatSupport.isXlsx(fileName, contentType);
    }

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        return parseLocally(fileId);
    }

    @Override
    protected LocalParseResult parseDownloadedFile(File downloadedFile, KnowledgeFileMetadata metadata) throws Exception {
        if (KnowledgeFormatSupport.isDoc(metadata.fileName(), metadata.contentType())) {
            return parseDoc(downloadedFile);
        }
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

    private LocalParseResult parseDoc(File downloadedFile) throws Exception {
        StringBuilder content = new StringBuilder();
        LegacyDocImageParseState imageState = new LegacyDocImageParseState();
        try (FileInputStream inputStream = new FileInputStream(downloadedFile);
             HWPFDocument document = new HWPFDocument(inputStream)) {
            Range range = document.getRange();
            PicturesTable picturesTable = document.getPicturesTable();
            TableIterator tableIterator = new TableIterator(range);
            int paragraphIndex = 0;
            int tableIndex = 1;
            while (paragraphIndex < range.numParagraphs()) {
                Paragraph paragraph = range.getParagraph(paragraphIndex);
                if (paragraph.isInTable() && tableIterator.hasNext()) {
                    Table table = tableIterator.next();
                    appendLegacyTable(content, table, tableIndex++, picturesTable, imageState);
                    paragraphIndex += table.numParagraphs();
                    continue;
                }
                appendLegacyParagraph(content, paragraph, picturesTable, imageState);
                paragraphIndex++;
            }
        }
        return new LocalParseResult(
                content.toString().strip(),
                DOC_CONTENT_TYPE,
                imageState.warnings,
                imageState.imageFileIds,
                imageState.hasImage && imageState.allImagesOcrParsed);
    }

    private LocalParseResult parseDocx(File downloadedFile) throws Exception {
        StringBuilder content = new StringBuilder();
        DocxImageParseState imageState = new DocxImageParseState();
        int tableIndex = 1;
        try (FileInputStream inputStream = new FileInputStream(downloadedFile);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            for (IBodyElement bodyElement : document.getBodyElements()) {
                if (bodyElement instanceof XWPFParagraph paragraph) {
                    appendParagraph(content, paragraph, imageState);
                    continue;
                }
                if (bodyElement instanceof XWPFTable table) {
                    appendTable(content, table, tableIndex++);
                }
            }
        }
        return new LocalParseResult(
                content.toString().strip(),
                DOCX_CONTENT_TYPE,
                imageState.warnings,
                imageState.imageFileIds,
                imageState.hasImage && imageState.allImagesOcrParsed);
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
        return new LocalParseResult(content.toString().strip(), PPTX_CONTENT_TYPE, List.of(), List.of(), false);
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
        return new LocalParseResult(content.toString().strip(), XLSX_CONTENT_TYPE, List.of(), List.of(), false);
    }

    private void appendParagraph(StringBuilder content, XWPFParagraph paragraph, DocxImageParseState imageState) {
        String text = buildParagraphContent(paragraph, imageState);
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

    private void appendLegacyParagraph(StringBuilder content,
                                       Paragraph paragraph,
                                       PicturesTable picturesTable,
                                       LegacyDocImageParseState imageState) {
        String text = buildLegacyParagraphContent(paragraph, picturesTable, imageState);
        if (!StringUtils.hasText(text)) {
            return;
        }
        appendBlock(content, "Paragraph: " + text);
    }

    private void appendLegacyTable(StringBuilder content,
                                   Table table,
                                   int tableIndex,
                                   PicturesTable picturesTable,
                                   LegacyDocImageParseState imageState) {
        appendBlock(content, "[Table " + tableIndex + "]");
        for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
            TableRow row = table.getRow(rowIndex);
            List<String> cells = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < row.numCells(); cellIndex++) {
                TableCell cell = row.getCell(cellIndex);
                String cellText = buildLegacyRangeContent(cell, picturesTable, imageState);
                if (StringUtils.hasText(cellText)) {
                    cells.add(cellText);
                }
            }
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

    private String buildParagraphContent(XWPFParagraph paragraph, DocxImageParseState imageState) {
        StringBuilder paragraphContent = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            appendRunText(paragraphContent, run);
            appendRunImages(paragraphContent, run, imageState);
        }
        return normalizeParagraphContent(paragraphContent.toString());
    }

    private String buildLegacyParagraphContent(Paragraph paragraph,
                                               PicturesTable picturesTable,
                                               LegacyDocImageParseState imageState) {
        return buildLegacyRangeContent(paragraph, picturesTable, imageState);
    }

    private String buildLegacyRangeContent(Range range,
                                           PicturesTable picturesTable,
                                           LegacyDocImageParseState imageState) {
        StringBuilder content = new StringBuilder();
        for (int runIndex = 0; runIndex < range.numCharacterRuns(); runIndex++) {
            org.apache.poi.hwpf.usermodel.CharacterRun run = range.getCharacterRun(runIndex);
            appendLegacyRunText(content, run);
            appendLegacyRunImages(content, run, picturesTable, imageState);
        }
        return normalizeParagraphContent(content.toString());
    }

    private void appendRunText(StringBuilder paragraphContent, XWPFRun run) {
        String text = normalizeText(run.text());
        if (!StringUtils.hasText(text)) {
            return;
        }
        appendInline(paragraphContent, text);
    }

    private void appendLegacyRunText(StringBuilder paragraphContent,
                                     org.apache.poi.hwpf.usermodel.CharacterRun run) {
        String text = normalizeLegacyText(run.text());
        if (!StringUtils.hasText(text)) {
            return;
        }
        appendInline(paragraphContent, text);
    }

    private void appendRunImages(StringBuilder paragraphContent, XWPFRun run, DocxImageParseState imageState) {
        CTR ctr = run.getCTR();
        if (ctr == null || ctr.sizeOfDrawingArray() == 0 || CollectionUtils.isEmpty(run.getEmbeddedPictures())) {
            return;
        }
        for (XWPFPicture picture : run.getEmbeddedPictures()) {
            XWPFPictureData pictureData = picture.getPictureData();
            if (pictureData == null) {
                continue;
            }
            byte[] imageBytes = pictureData.getData();
            if (imageBytes == null || imageBytes.length == 0) {
                continue;
            }
            appendInlineImage(paragraphContent,
                    imageBytes,
                    resolveDocxImageFileName(pictureData, imageState.imageFileIds.size() + 1),
                    pictureData.getPackagePart().getContentType(),
                    imageState);
        }
    }

    private void appendLegacyRunImages(StringBuilder paragraphContent,
                                       org.apache.poi.hwpf.usermodel.CharacterRun run,
                                       PicturesTable picturesTable,
                                       LegacyDocImageParseState imageState) {
        if (picturesTable == null || run == null || !picturesTable.hasPicture(run)) {
            return;
        }
        Picture picture = picturesTable.extractPicture(run, false);
        if (picture == null) {
            return;
        }
        byte[] imageBytes = picture.getContent();
        if (imageBytes == null || imageBytes.length == 0) {
            return;
        }
        appendInlineImage(paragraphContent,
                imageBytes,
                resolveLegacyDocImageFileName(picture, imageState.imageFileIds.size() + 1),
                picture.getMimeType(),
                imageState);
    }

    private void appendInlineImage(StringBuilder paragraphContent,
                                   byte[] imageBytes,
                                   String fileName,
                                   String contentType,
                                   AbstractImageParseState imageState) {
        String fileId = knowledgeImageUploadService.upload(imageBytes, fileName, contentType);
        if (!StringUtils.hasText(fileId)) {
            return;
        }
        imageState.hasImage = true;
        imageState.imageFileIds.add(fileId);
        KnowledgeImageOcrResult ocrResult = knowledgeImageOcrService.recognize(imageBytes, fileName, contentType);
        if (StringUtils.hasText(ocrResult.warning())) {
            imageState.warnings.add(ocrResult.warning());
        }
        if (!ocrResult.parsed()) {
            imageState.allImagesOcrParsed = false;
        }
        appendInline(paragraphContent, KnowledgeImageMarkerSupport.marker(fileId, ocrResult.altText()));
    }

    private String resolveDocxImageFileName(XWPFPictureData pictureData, int index) {
        String fileName = pictureData.getFileName();
        if (StringUtils.hasText(fileName)) {
            return fileName;
        }
        String extension = pictureData.suggestFileExtension();
        if (!StringUtils.hasText(extension)) {
            extension = "png";
        }
        return "knowledge-docx-image-%s.%s".formatted(index, extension);
    }

    private String resolveLegacyDocImageFileName(Picture picture, int index) {
        String fileName = picture.suggestFullFileName();
        if (StringUtils.hasText(fileName)) {
            return fileName;
        }
        String extension = picture.suggestFileExtension();
        if (!StringUtils.hasText(extension)) {
            extension = "png";
        }
        return "knowledge-doc-image-%s.%s".formatted(index, extension);
    }

    private void appendInline(StringBuilder paragraphContent, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!paragraphContent.isEmpty() && !Character.isWhitespace(paragraphContent.charAt(paragraphContent.length() - 1))) {
            paragraphContent.append(' ');
        }
        paragraphContent.append(value.trim());
    }

    private String normalizeParagraphContent(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeLegacyText(String text) {
        if (text == null) {
            return "";
        }
        return normalizeText(text
                .replace('\u0007', ' ')
                .replace('\u0001', ' ')
                .replace('\u0013', ' ')
                .replace('\u0014', ' ')
                .replace('\u0015', ' '));
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

    private static class AbstractImageParseState {

        protected final List<String> imageFileIds = new ArrayList<>();

        protected final List<String> warnings = new ArrayList<>();

        protected boolean hasImage = false;

        protected boolean allImagesOcrParsed = true;
    }

    private static final class DocxImageParseState extends AbstractImageParseState {
    }

    private static final class LegacyDocImageParseState extends AbstractImageParseState {
    }
}
