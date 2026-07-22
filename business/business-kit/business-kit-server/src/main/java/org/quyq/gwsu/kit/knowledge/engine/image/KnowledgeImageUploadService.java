package org.quyq.gwsu.kit.knowledge.engine.image;

import org.quyq.gwsu.kit.api.file.dto.FileProperty;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.ChunkMultipartFile;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 知识库导入图片上传服务。
 */
@Service
public class KnowledgeImageUploadService {

    private static final String DEFAULT_FILE_NAME = "knowledge-image";

    public String upload(byte[] bytes, String fileName, String contentType) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String resolvedFileName = StringUtils.hasText(fileName) ? fileName : DEFAULT_FILE_NAME;
        String resolvedContentType = StringUtils.hasText(contentType) ? contentType : "image/png";
        KitFileInfoVO fileInfo = FileUtils.upload(
                new ChunkMultipartFile(resolvedFileName, resolvedContentType, bytes),
                FileProperty.builder()
                        .scopePublic()
                        .categorize("knowledge_img")
                        .build());
        return fileInfo == null ? "" : fileInfo.getFileId();
    }
}
