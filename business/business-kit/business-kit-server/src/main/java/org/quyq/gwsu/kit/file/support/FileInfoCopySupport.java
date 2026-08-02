package org.quyq.gwsu.kit.file.support;

import org.quyq.gwsu.kit.api.file.dto.FileCopyDTO;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.file.domain.KitFileInfo;
import org.springframework.util.StringUtils;

import java.util.Optional;

public final class FileInfoCopySupport {

    private FileInfoCopySupport() {
    }

    public static KitFileInfo buildCopiedFileInfo(KitFileInfoVO source, FileCopyDTO form) {
        KitFileInfo fileInfo = new KitFileInfo();
        fileInfo.setFileMetaId(source.getFileMetaId());
        fileInfo.setFileSize(source.getFileSize());

        String resolvedFileName = resolveFileName(source, form);
        String fileSuffix = getLastSuffix(resolvedFileName);
        fileInfo.setFileSuffix(StringUtils.hasText(fileSuffix) ? fileSuffix : source.getFileSuffix());
        fileInfo.setFileName(
                StringUtils.hasText(fileSuffix)
                        ? resolvedFileName.substring(0, resolvedFileName.length() - fileSuffix.length() - 1)
                        : resolvedFileName
        );
        fileInfo.setDisposable(Optional.ofNullable(form.getDisposable()).orElse(false));
        fileInfo.setScope(Optional.ofNullable(form.getScope()).orElse(FileScope.PROTECTED));
        fileInfo.setVisitors(form.getVisitors());
        fileInfo.setExpiredTime(form.getExpiredTime());
        return fileInfo;
    }

    private static String resolveFileName(KitFileInfoVO source, FileCopyDTO form) {
        if (StringUtils.hasText(form.getFileName())) {
            return form.getFileName().trim();
        }
        if (StringUtils.hasText(source.getFileSuffix())) {
            return source.getFileName() + "." + source.getFileSuffix();
        }
        return source.getFileName();
    }

    private static String getLastSuffix(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
