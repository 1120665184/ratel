package org.quyq.gwsu.kit.knowledge.engine;

/**
 * 知识源文件的解析路由元数据。
 *
 * @param fileName 文件名
 * @param contentType 媒体类型
 */
public record KnowledgeFileMetadata(String fileName, String contentType) {

    public KnowledgeFileMetadata {
        fileName = fileName == null ? "" : fileName;
        contentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }
}
