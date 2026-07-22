package org.quyq.gwsu.kit.knowledge.engine.ingest;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.quyq.gwsu.kit.knowledge.engine.support.KnowledgeFileMetadata;
import org.quyq.gwsu.kit.knowledge.engine.support.KnowledgeFileMetadataResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 按文件类型路由并在专用解析失败时回退到 Tika 的主解析器。
 */
@Component
@Primary
public class CompositeKnowledgeDocumentParser implements KnowledgeDocumentParser {

    private static final double MIN_VISIBLE_CHARACTER_RATIO = 0.1D;

    private static final Pattern STRUCTURAL_ONLY_LINE = Pattern.compile(
            "^(?:Page \\d+|Slide \\d+|Sheet \\d+:.*|\\[Table \\d+])$");

    private static final Pattern STRUCTURAL_PREFIX = Pattern.compile(
            "^(?:Title|Paragraph|Content|Row \\d+|Table Row):\\s*");

    private final KnowledgeDocumentParser pdfParser;

    private final KnowledgeDocumentParser officeParser;

    private final KnowledgeDocumentParser tikaParser;

    private final KnowledgeFileMetadataResolver metadataResolver;

    public CompositeKnowledgeDocumentParser(
            @Qualifier("pdfKnowledgeDocumentParser") KnowledgeDocumentParser pdfParser,
            @Qualifier("officeKnowledgeDocumentParser") KnowledgeDocumentParser officeParser,
            @Qualifier("tikaKnowledgeDocumentParser") KnowledgeDocumentParser tikaParser,
            KnowledgeFileMetadataResolver metadataResolver) {
        this.pdfParser = pdfParser;
        this.officeParser = officeParser;
        this.tikaParser = tikaParser;
        this.metadataResolver = metadataResolver;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        return true;
    }

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        KnowledgeFileMetadata metadata = metadataResolver.resolve(fileId);
        KnowledgeDocumentParser specializedParser = resolveSpecializedParser(metadata);
        if (specializedParser == null) {
            return normalizeMetadata(tikaParser.parse(fileId), metadata, List.of());
        }

        try {
            ParsedKnowledgeDocument parsedDocument = specializedParser.parse(fileId);
            String qualityProblem = qualityProblem(parsedDocument.text());
            if (qualityProblem == null) {
                return normalizeMetadata(parsedDocument, metadata, List.of());
            }
            return fallbackToTika(fileId, metadata,
                    parserName(specializedParser) + " 专用解析结果质量不合格（" + qualityProblem + "），已回退 Tika 解析。");
        } catch (Exception ex) {
            return fallbackToTika(fileId, metadata,
                    parserName(specializedParser) + " 专用解析失败（" + safeMessage(ex) + "），已回退 Tika 解析。");
        }
    }

    private KnowledgeDocumentParser resolveSpecializedParser(KnowledgeFileMetadata metadata) {
        if (pdfParser.supports(metadata.fileName(), metadata.contentType())) {
            return pdfParser;
        }
        if (officeParser.supports(metadata.fileName(), metadata.contentType())) {
            return officeParser;
        }
        return null;
    }

    private ParsedKnowledgeDocument fallbackToTika(String fileId, KnowledgeFileMetadata metadata, String warning) {
        ParsedKnowledgeDocument fallback = tikaParser.parse(fileId);
        return normalizeMetadata(fallback, metadata, List.of(warning));
    }

    private ParsedKnowledgeDocument normalizeMetadata(ParsedKnowledgeDocument parsedDocument,
                                                       KnowledgeFileMetadata metadata,
                                                       List<String> precedingWarnings) {
        List<String> warnings = new ArrayList<>(precedingWarnings);
        warnings.addAll(parsedDocument.parseWarnings());
        String fileName = StringUtils.hasText(parsedDocument.fileName()) ? parsedDocument.fileName() : metadata.fileName();
        String contentType = StringUtils.hasText(parsedDocument.contentType())
                && !"application/octet-stream".equals(parsedDocument.contentType())
                ? parsedDocument.contentType() : metadata.contentType();
        return new ParsedKnowledgeDocument(
                fileName,
                contentType,
                parsedDocument.sourceLanguage(),
                parsedDocument.text(),
                warnings,
                parsedDocument.imageFileIds(),
                parsedDocument.imageOcrParsed());
    }

    private String qualityProblem(String text) {
        if (!StringUtils.hasText(text)) {
            return "未提取到有效可见字符";
        }
        String normalizedText = removeStructuralMarkers(text);
        if (!StringUtils.hasText(normalizedText)) {
            return "仅提取到结构边界标记";
        }
        long nonWhitespaceCount = normalizedText.codePoints().filter(codePoint -> !Character.isWhitespace(codePoint)).count();
        long visibleCount = normalizedText.codePoints().filter(this::isVisibleCharacter).count();
        if (visibleCount == 0) {
            return "未提取到有效可见字符";
        }
        if (nonWhitespaceCount == 0 || (double) visibleCount / nonWhitespaceCount < MIN_VISIBLE_CHARACTER_RATIO) {
            return "有效字符占比过低";
        }
        return null;
    }

    private boolean isVisibleCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint)
                || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    private String removeStructuralMarkers(String text) {
        return text.lines()
                .map(String::strip)
                .filter(StringUtils::hasText)
                .filter(line -> !STRUCTURAL_ONLY_LINE.matcher(line).matches())
                .map(line -> STRUCTURAL_PREFIX.matcher(line).replaceFirst(""))
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + '\n' + right)
                .orElse("");
    }

    private String parserName(KnowledgeDocumentParser parser) {
        return parser == pdfParser ? "PDF" : "Office";
    }

    private String safeMessage(Exception exception) {
        return exception.getClass().getSimpleName();
    }
}
