package org.quyq.gwsu.kit.knowledge.engine.ingest;

/**
 * 知识源文档解析器。
 */
public interface KnowledgeDocumentParser {

    /**
     * 判断解析器是否适用于指定文件。
     *
     * @param fileName 文件名，可能为空或为文件 ID
     * @param contentType 媒体类型，未知时为 application/octet-stream
     * @return 是否支持
     */
    boolean supports(String fileName, String contentType);

    ParsedKnowledgeDocument parse(String fileId);
}
