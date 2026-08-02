package org.quyq.gwsu.kit.file.support;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.file.dto.FileCopyDTO;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.file.domain.KitFileInfo;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FileInfoCopySupportTest {

    @Test
    void shouldReuseMetaAndOriginalFileNameWhenCopyingFile() {
        KitFileInfoVO source = new KitFileInfoVO();
        source.setFileMetaId("meta-1");
        source.setFileName("agent-guide");
        source.setFileSuffix("pdf");
        source.setFileSize("1024");

        FileCopyDTO dto = new FileCopyDTO();
        dto.setSourceFileId("file-source");

        KitFileInfo copied = FileInfoCopySupport.buildCopiedFileInfo(source, dto);

        assertEquals("meta-1", copied.getFileMetaId());
        assertEquals("agent-guide", copied.getFileName());
        assertEquals("pdf", copied.getFileSuffix());
        assertEquals("1024", copied.getFileSize());
        assertFalse(copied.getDisposable());
        assertEquals(FileScope.PROTECTED, copied.getScope());
    }

    @Test
    void shouldApplyOverridePropertiesWhenCopyingFile() {
        KitFileInfoVO source = new KitFileInfoVO();
        source.setFileMetaId("meta-2");
        source.setFileName("draft");
        source.setFileSuffix("docx");
        source.setFileSize("2048");

        LocalDateTime expiredTime = LocalDateTime.of(2026, 8, 3, 10, 0, 0);
        FileCopyDTO dto = new FileCopyDTO();
        dto.setSourceFileId("file-source");
        dto.setFileName("knowledge-final.md");
        dto.setDisposable(true);
        dto.setScope(FileScope.PRIVATE);
        dto.setVisitors("u1,u2");
        dto.setExpiredTime(expiredTime);

        KitFileInfo copied = FileInfoCopySupport.buildCopiedFileInfo(source, dto);

        assertEquals("meta-2", copied.getFileMetaId());
        assertEquals("knowledge-final", copied.getFileName());
        assertEquals("md", copied.getFileSuffix());
        assertEquals("2048", copied.getFileSize());
        assertEquals(true, copied.getDisposable());
        assertEquals(FileScope.PRIVATE, copied.getScope());
        assertEquals("u1,u2", copied.getVisitors());
        assertEquals(expiredTime, copied.getExpiredTime());
    }
}
