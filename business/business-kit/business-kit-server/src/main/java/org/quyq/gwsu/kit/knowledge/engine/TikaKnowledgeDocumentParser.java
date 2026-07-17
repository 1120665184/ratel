package org.quyq.gwsu.kit.knowledge.engine;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * 基于 Tika 的知识源文档解析器。
 */
@Component("tikaKnowledgeDocumentParser")
public class TikaKnowledgeDocumentParser extends AbstractLocalKnowledgeDocumentParser {

    private final Tika tika = new Tika();

    public TikaKnowledgeDocumentParser(KnowledgeFileMetadataResolver metadataResolver) {
        super(metadataResolver);
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        return true;
    }

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        return parseLocally(fileId);
    }

    @Override
    protected LocalParseResult parseDownloadedFile(File downloadedFile, KnowledgeFileMetadata metadata) throws Exception {
        String text;
        try (FileInputStream inputStream = new FileInputStream(downloadedFile)) {
            text = tika.parseToString(inputStream);
        }
        return new LocalParseResult(StringUtils.hasText(text) ? text : "", metadata.contentType(), List.of());
    }
}
