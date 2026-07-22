package org.quyq.gwsu.kit.knowledge.engine.support;

/**
 * 获取知识源文件用于解析路由的元数据。
 */
@FunctionalInterface
public interface KnowledgeFileMetadataResolver {

    /**
     * 解析文件元数据。
     *
     * @param fileId 文件 ID
     * @return 文件元数据
     */
    KnowledgeFileMetadata resolve(String fileId);
}
