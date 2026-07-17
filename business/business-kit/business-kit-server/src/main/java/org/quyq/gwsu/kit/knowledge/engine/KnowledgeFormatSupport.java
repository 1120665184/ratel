package org.quyq.gwsu.kit.knowledge.engine;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * 知识文档格式判定工具。
 */
final class KnowledgeFormatSupport {

    private static final Set<String> PDF_CONTENT_TYPES = Set.of("application/pdf");

    private static final Set<String> DOCX_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> PPTX_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    private static final Set<String> XLSX_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

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

    private KnowledgeFormatSupport() {
    }

    static boolean isPdf(String fileName, String contentType) {
        return matchesContentType(contentType, PDF_CONTENT_TYPES) || hasExtension(fileName, ".pdf");
    }

    static boolean isOffice(String fileName, String contentType) {
        if (matchesContentType(contentType, OFFICE_CONTENT_TYPES)) {
            return true;
        }
        String lowerCaseFileName = normalize(fileName);
        return OFFICE_EXTENSIONS.stream().anyMatch(lowerCaseFileName::endsWith);
    }

    static boolean isDocx(String fileName, String contentType) {
        return matchesContentType(contentType, DOCX_CONTENT_TYPES) || hasExtension(fileName, ".docx");
    }

    static boolean isPptx(String fileName, String contentType) {
        return matchesContentType(contentType, PPTX_CONTENT_TYPES) || hasExtension(fileName, ".pptx");
    }

    static boolean isXlsx(String fileName, String contentType) {
        return matchesContentType(contentType, XLSX_CONTENT_TYPES) || hasExtension(fileName, ".xlsx");
    }

    private static boolean matchesContentType(String contentType, Set<String> contentTypes) {
        return StringUtils.hasText(contentType) && contentTypes.contains(contentType.toLowerCase(Locale.ROOT));
    }

    private static boolean hasExtension(String fileName, String extension) {
        return normalize(fileName).endsWith(extension);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
