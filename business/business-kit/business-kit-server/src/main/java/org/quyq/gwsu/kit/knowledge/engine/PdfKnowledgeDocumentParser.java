package org.quyq.gwsu.kit.knowledge.engine;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.quyq.gwsu.kit.config.properties.KnowledgePdfParseProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 专用解析器。
 *
 * <p>默认使用 PDFBox 做本地分页提取；增强模式未注册本地策略时仍严格在本地回退，绝不上传文件。</p>
 */
@Component("pdfKnowledgeDocumentParser")
public class PdfKnowledgeDocumentParser extends AbstractLocalKnowledgeDocumentParser {

    private final KnowledgePdfParseProperties properties;

    private final ObjectProvider<PdfEnhancedParseStrategy> enhancedParseStrategyProvider;

    public PdfKnowledgeDocumentParser(KnowledgeFileMetadataResolver metadataResolver,
                                      KnowledgePdfParseProperties properties,
                                      ObjectProvider<PdfEnhancedParseStrategy> enhancedParseStrategyProvider) {
        super(metadataResolver);
        this.properties = properties;
        this.enhancedParseStrategyProvider = enhancedParseStrategyProvider;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        return KnowledgeFormatSupport.isPdf(fileName, contentType);
    }

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        PdfEnhancedParseStrategy enhancedParseStrategy = properties.getMode() == PdfParseMode.ENHANCED
                ? enhancedParseStrategyProvider.orderedStream().findFirst().orElse(null)
                : null;
        if (enhancedParseStrategy != null) {
            return enhancedParseStrategy.parse(fileId);
        }
        ParsedKnowledgeDocument parsedDocument = parseLocally(fileId);
        if (properties.getMode() != PdfParseMode.ENHANCED) {
            return parsedDocument;
        }
        List<String> warnings = new ArrayList<>(parsedDocument.parseWarnings());
        warnings.add("PDF 增强解析模式未配置本地增强策略，已回退本地 PDFBox 解析。");
        return new ParsedKnowledgeDocument(
                parsedDocument.fileName(),
                parsedDocument.contentType(),
                parsedDocument.sourceLanguage(),
                parsedDocument.text(),
                warnings);
    }

    @Override
    protected LocalParseResult parseDownloadedFile(File downloadedFile, KnowledgeFileMetadata metadata) throws Exception {
        List<String> warnings = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        boolean hasTextPage = false;
        try (PDDocument document = PDDocument.load(downloadedFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int pageIndex = 1; pageIndex <= document.getNumberOfPages(); pageIndex++) {
                stripper.setStartPage(pageIndex);
                stripper.setEndPage(pageIndex);
                String pageText = stripper.getText(document).strip();
                if (content.length() > 0) {
                    content.append("\n\n");
                }
                content.append("Page ").append(pageIndex);
                if (StringUtils.hasText(pageText)) {
                    hasTextPage = true;
                    content.append('\n').append(pageText);
                } else {
                    warnings.add("Page " + pageIndex + " 未提取到文本内容。");
                }
            }
        }
        return new LocalParseResult(hasTextPage ? content.toString().strip() : "", "application/pdf", warnings);
    }
}
