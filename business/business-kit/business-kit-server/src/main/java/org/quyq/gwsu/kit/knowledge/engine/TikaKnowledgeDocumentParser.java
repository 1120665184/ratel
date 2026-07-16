package org.quyq.gwsu.kit.knowledge.engine;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Optional;

/**
 * 基于 Tika 的知识源文档解析器。
 */
@Component
@RequiredArgsConstructor
public class TikaKnowledgeDocumentParser implements KnowledgeDocumentParser {

    private static final String DOWNLOAD_ROOT_PATH = System.getProperty("java.io.tmpdir")
            + File.separator + "gwsu-knowledge-parser";

    private final Tika tika = new Tika();

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        File downloadedFile = null;
        try {
            downloadedFile = FileUtils.download(fileId, DOWNLOAD_ROOT_PATH);
            if (downloadedFile == null || !downloadedFile.exists() || downloadedFile.length() == 0) {
                throw new BusinessException(KitErrorCode.E03006);
            }
            String text;
            try (FileInputStream inputStream = new FileInputStream(downloadedFile)) {
                text = tika.parseToString(inputStream);
            }
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(KitErrorCode.E03006);
            }
            return new ParsedKnowledgeDocument(resolveFileName(fileId), text);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(KitErrorCode.E03007, ex);
        } finally {
            deleteDownloadedFile(downloadedFile);
        }
    }

    private String resolveFileName(String fileId) {
        try {
            KitFileInfoVO fileInfo = FileUtils.getFileInfo(fileId);
            return Optional.ofNullable(fileInfo)
                    .map(KitFileInfoVO::getFileName)
                    .filter(StringUtils::hasText)
                    .orElse(fileId);
        } catch (Exception ex) {
            return fileId;
        }
    }

    private void deleteDownloadedFile(File downloadedFile) {
        if (downloadedFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(downloadedFile.toPath());
        } catch (Exception ignored) {
            // 解析临时文件清理失败不影响导入结果，后续任务可复用同一文件名覆盖。
        }
    }
}
