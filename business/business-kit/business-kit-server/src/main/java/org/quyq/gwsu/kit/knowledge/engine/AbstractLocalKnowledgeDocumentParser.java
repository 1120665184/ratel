package org.quyq.gwsu.kit.knowledge.engine;

import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 基于本地临时文件下载的知识文档解析基类。
 */
abstract class AbstractLocalKnowledgeDocumentParser implements KnowledgeDocumentParser {

    private final KnowledgeFileMetadataResolver metadataResolver;

    protected AbstractLocalKnowledgeDocumentParser(KnowledgeFileMetadataResolver metadataResolver) {
        this.metadataResolver = metadataResolver;
    }

    protected final ParsedKnowledgeDocument parseLocally(String fileId) {
        try (LocalDownloadedKnowledgeFile localFile = openLocalFile(fileId)) {
            LocalParseResult result = parseDownloadedFile(localFile.file(), localFile.metadata());
            if (!StringUtils.hasText(result.text())) {
                throw new BusinessException(KitErrorCode.E03006);
            }
            return new ParsedKnowledgeDocument(
                    localFile.metadata().fileName(),
                    resolveContentType(localFile.metadata(), result.contentType()),
                    detectLanguage(result.text()),
                    result.text(),
                    result.warnings());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(KitErrorCode.E03007, ex);
        }
    }

    protected abstract LocalParseResult parseDownloadedFile(File downloadedFile, KnowledgeFileMetadata metadata) throws Exception;

    protected LocalDownloadedKnowledgeFile openLocalFile(String fileId) throws Exception {
        Path temporaryDirectory = Files.createTempDirectory("gwsu-knowledge-parser-");
        try {
            File downloadedFile = FileUtils.download(fileId, temporaryDirectory.toString());
            if (downloadedFile == null || !downloadedFile.exists() || downloadedFile.length() == 0) {
                throw new BusinessException(KitErrorCode.E03006);
            }
            return LocalDownloadedKnowledgeFile.downloaded(downloadedFile, temporaryDirectory, resolveMetadata(fileId));
        } catch (Exception ex) {
            deleteTemporaryDirectory(temporaryDirectory);
            throw ex;
        }
    }

    protected KnowledgeFileMetadata resolveMetadata(String fileId) {
        try {
            return metadataResolver.resolve(fileId);
        } catch (Exception ignored) {
            return new KnowledgeFileMetadata(resolveFileName(fileId), resolveContentType(fileId));
        }
    }

    protected String detectLanguage(String text) {
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
        String lowerCase = text.toLowerCase(Locale.ROOT);
        if (lowerCase.matches("(?s).*\\b(le|la|les|des|une|dans|pour|avec)\\b.*")) {
            return "fr";
        }
        return "en";
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

    private String resolveContentType(KnowledgeFileMetadata metadata, String contentTypeOverride) {
        if (StringUtils.hasText(contentTypeOverride)) {
            return contentTypeOverride;
        }
        return metadata.contentType();
    }

    private static void deleteDownloadedFile(File downloadedFile) {
        if (downloadedFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(downloadedFile.toPath());
        } catch (Exception ignored) {
            // 解析临时文件清理失败不影响导入结果。
        }
    }

    private static void deleteTemporaryDirectory(Path temporaryDirectory) {
        if (temporaryDirectory == null) {
            return;
        }
        try (var paths = Files.walk(temporaryDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // 临时目录清理失败不影响解析结果。
                }
            });
        } catch (Exception ignored) {
            // 目录遍历失败不影响已经完成的解析。
        }
    }

    protected record LocalParseResult(String text, String contentType, List<String> warnings) {

        protected LocalParseResult {
            text = text == null ? "" : text;
            contentType = contentType == null ? "" : contentType;
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    protected static final class LocalDownloadedKnowledgeFile implements AutoCloseable {

        private final File file;

        private final Path temporaryDirectory;

        private final KnowledgeFileMetadata metadata;

        private final boolean cleanupFile;

        private LocalDownloadedKnowledgeFile(File file,
                                             Path temporaryDirectory,
                                             KnowledgeFileMetadata metadata,
                                             boolean cleanupFile) {
            this.file = file;
            this.temporaryDirectory = temporaryDirectory;
            this.metadata = metadata;
            this.cleanupFile = cleanupFile;
        }

        static LocalDownloadedKnowledgeFile downloaded(File file, Path temporaryDirectory, KnowledgeFileMetadata metadata) {
            return new LocalDownloadedKnowledgeFile(file, temporaryDirectory, metadata, true);
        }

        protected static LocalDownloadedKnowledgeFile existing(File file, KnowledgeFileMetadata metadata) {
            return new LocalDownloadedKnowledgeFile(file, null, metadata, false);
        }

        File file() {
            return file;
        }

        KnowledgeFileMetadata metadata() {
            return metadata;
        }

        @Override
        public void close() {
            if (cleanupFile) {
                deleteDownloadedFile(file);
            }
            deleteTemporaryDirectory(temporaryDirectory);
        }
    }
}
