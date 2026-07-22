package org.quyq.gwsu.kit.knowledge.engine.support;

import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 基于文件工具获取知识源文件元数据。
 */
@Component
public class FileUtilsKnowledgeFileMetadataResolver implements KnowledgeFileMetadataResolver {

    @Override
    public KnowledgeFileMetadata resolve(String fileId) {
        try {
            KitFileInfoVO fileInfo = FileUtils.getFileInfo(fileId);
            if (fileInfo == null) {
                return new KnowledgeFileMetadata(fileId, "application/octet-stream");
            }
            String fileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : fileId;
            String contentType = StringUtils.hasText(fileInfo.getMediaType())
                    ? fileInfo.getMediaType() : "application/octet-stream";
            return new KnowledgeFileMetadata(fileName, contentType);
        } catch (Exception ignored) {
            return new KnowledgeFileMetadata(fileId, "application/octet-stream");
        }
    }
}
