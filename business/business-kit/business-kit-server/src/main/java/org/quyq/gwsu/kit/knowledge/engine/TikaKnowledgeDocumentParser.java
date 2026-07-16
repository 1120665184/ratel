package org.quyq.gwsu.kit.knowledge.engine;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.FileClientApi;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * 基于 Tika 的知识源文档解析器。
 */
@Component
@RequiredArgsConstructor
public class TikaKnowledgeDocumentParser implements KnowledgeDocumentParser {

    private final FileClientApi fileClientApi;

    private final Tika tika = new Tika();

    @Override
    public ParsedKnowledgeDocument parse(String fileId) {
        try {
            byte[] data = fileClientApi.download(fileId, null);
            if (data == null || data.length == 0) {
                throw new BusinessException(KitErrorCode.E03006);
            }
            String text = tika.parseToString(new ByteArrayInputStream(data));
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(KitErrorCode.E03006);
            }
            return new ParsedKnowledgeDocument(resolveFileName(fileId), text);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(KitErrorCode.E03007, ex);
        }
    }

    private String resolveFileName(String fileId) {
        try {
            R<KitFileInfoVO> fileInfo = fileClientApi.getFileInfo(fileId);
            return Optional.ofNullable(fileInfo)
                    .filter(R::isSuccess)
                    .map(R::data)
                    .map(KitFileInfoVO::getFileName)
                    .filter(StringUtils::hasText)
                    .orElse(fileId);
        } catch (Exception ex) {
            return fileId;
        }
    }
}
