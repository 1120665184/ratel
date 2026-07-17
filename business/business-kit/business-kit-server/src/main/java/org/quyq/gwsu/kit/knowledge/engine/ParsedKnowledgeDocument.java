package org.quyq.gwsu.kit.knowledge.engine;

import java.util.List;

/**
 * 已解析的知识源文档。
 */
public record ParsedKnowledgeDocument(String fileName,
                                      String contentType,
                                      String sourceLanguage,
                                      String text,
                                      List<String> parseWarnings) {

    public ParsedKnowledgeDocument {
        fileName = fileName == null ? "" : fileName;
        contentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        sourceLanguage = sourceLanguage == null || sourceLanguage.isBlank() ? "und" : sourceLanguage;
        text = text == null ? "" : text;
        parseWarnings = parseWarnings == null ? List.of() : List.copyOf(parseWarnings);
    }

    /**
     * 为无需额外元数据的本地解析结果创建默认值。
     *
     * @param fileName 文件名
     * @param text 已解析文本
     * @return 已解析文档
     */
    public static ParsedKnowledgeDocument of(String fileName, String text) {
        return new ParsedKnowledgeDocument(fileName, "application/octet-stream", "und", text, List.of());
    }
}
