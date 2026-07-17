package org.quyq.gwsu.kit.knowledge.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Office 文档专用解析器，使用 Tika 在本地提取 Word、Excel、PowerPoint 内容。
 */
@Component("officeKnowledgeDocumentParser")
@RequiredArgsConstructor
public class OfficeKnowledgeDocumentParser implements KnowledgeDocumentParser {

    private static final Set<String> OFFICE_CONTENT_TYPES = Set.of(
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation"
    );

    private static final Set<String> OFFICE_EXTENSIONS = Set.of(
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".odt", ".ods", ".odp"
    );

    private final @Qualifier("tikaKnowledgeDocumentParser") TikaKnowledgeDocumentParser tikaParser;

    @Override
    public boolean supports(String fileName, String contentType) {
        if (OFFICE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return true;
        }
        String lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);
        return OFFICE_EXTENSIONS.stream().anyMatch(lowerCaseFileName::endsWith);
    }

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        return tikaParser.parse(fileId);
    }
}
