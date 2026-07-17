package org.quyq.gwsu.kit.knowledge.engine;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.config.properties.KnowledgePdfParseProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PDF 专用解析器。
 *
 * <p>当前使用 Tika 的本地 PDF 解析能力，增强模式未注册本地策略时仍严格在本地回退，绝不上传文件。</p>
 */
@Component("pdfKnowledgeDocumentParser")
@RequiredArgsConstructor
public class PdfKnowledgeDocumentParser implements KnowledgeDocumentParser {

    private final @Qualifier("tikaKnowledgeDocumentParser") TikaKnowledgeDocumentParser tikaParser;

    private final KnowledgePdfParseProperties properties;

    private final ObjectProvider<PdfEnhancedParseStrategy> enhancedParseStrategyProvider;

    @Override
    public boolean supports(String fileName, String contentType) {
        return "application/pdf".equalsIgnoreCase(contentType)
                || fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        ParsedKnowledgeDocument parsedDocument = tikaParser.parse(fileId);
        if (properties.getMode() != PdfParseMode.ENHANCED) {
            return parsedDocument;
        }
        PdfEnhancedParseStrategy enhancedParseStrategy = enhancedParseStrategyProvider.orderedStream().findFirst().orElse(null);
        if (enhancedParseStrategy != null) {
            return enhancedParseStrategy.parse(fileId);
        }
        List<String> warnings = new ArrayList<>(parsedDocument.parseWarnings());
        warnings.add("PDF 增强解析模式未配置本地增强策略，已回退本地解析。");
        return new ParsedKnowledgeDocument(parsedDocument.fileName(), parsedDocument.contentType(),
                parsedDocument.sourceLanguage(), parsedDocument.text(), warnings);
    }
}
