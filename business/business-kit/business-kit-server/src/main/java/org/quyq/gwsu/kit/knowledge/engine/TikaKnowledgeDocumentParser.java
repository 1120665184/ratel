package org.quyq.gwsu.kit.knowledge.engine;

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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

/**
 * 基于 Tika 的知识源文档解析器。
 */
@Component("tikaKnowledgeDocumentParser")
public class TikaKnowledgeDocumentParser implements KnowledgeDocumentParser {

    private final Tika tika = new Tika();

    @Override
    public boolean supports(String fileName, String contentType) {
        return true;
    }

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        File downloadedFile = null;
        Path temporaryDirectory = null;
        try {
            temporaryDirectory = Files.createTempDirectory("gwsu-knowledge-parser-");
            downloadedFile = FileUtils.download(fileId, temporaryDirectory.toString());
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
            String fileName = resolveFileName(fileId);
            return new ParsedKnowledgeDocument(fileName, resolveContentType(fileId), detectLanguage(text), text, List.of());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(KitErrorCode.E03007, ex);
        } finally {
            deleteDownloadedFile(downloadedFile);
            deleteTemporaryDirectory(temporaryDirectory);
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

    private String resolveContentType(String fileId) {
        try {
            KitFileInfoVO fileInfo = FileUtils.getFileInfo(fileId);
            return Optional.ofNullable(fileInfo)
                    .map(KitFileInfoVO::getMediaType)
                    .filter(StringUtils::hasText)
                    .orElse("application/octet-stream");
        } catch (Exception ex) {
            return "application/octet-stream";
        }
    }

    private String detectLanguage(String text) {
        long cjkCount = text.codePoints()
                .filter(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)
                .count();
        if (cjkCount > 0) {
            return "zh";
        }
        long latinCount = text.codePoints()
                .filter(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN)
                .count();
        if (latinCount == 0) {
            return "und";
        }
        String lowerCase = text.toLowerCase(java.util.Locale.ROOT);
        if (lowerCase.matches("(?s).*\\b(le|la|les|des|une|dans|pour|avec)\\b.*")) {
            return "fr";
        }
        return "en";
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

    private void deleteTemporaryDirectory(Path temporaryDirectory) {
        if (temporaryDirectory == null) {
            return;
        }
        try (var paths = Files.walk(temporaryDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // 临时目录清理失败不影响解析结果，由运行环境的临时目录回收策略兜底。
                }
            });
        } catch (Exception ignored) {
            // 目录遍历失败不影响已经完成的解析。
        }
    }
}
