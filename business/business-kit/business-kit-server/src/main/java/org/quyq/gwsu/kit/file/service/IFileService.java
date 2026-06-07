package org.quyq.gwsu.kit.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.dto.FileUploadDTO;
import org.quyq.gwsu.kit.api.file.dto.FileStreamWrapper;
import org.quyq.gwsu.kit.api.file.dto.KitFileInfoDTO;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.file.domain.KitFileInfo;

import java.util.List;
import java.util.Map;

public interface IFileService {

    IPage<KitFileInfoVO> pageByCondition(KitFileInfoDTO form);

    KitFileInfoVO getById(String id);

    KitFileInfo upload(FileUploadDTO form);

    FileStreamWrapper download(String fileId, String range);

    void remove(String fileId);

    FileServiceType getServerType();

    default Map<String, Object> createMultipartUpload(ChunkMultipartDTO form) {
        throw new BusinessException(KitErrorCode.E01001);
    }

    default KitFileInfo completeMultipartUpload(ChunkMultipartDTO form) {
        throw new BusinessException(KitErrorCode.E01001);
    }

    default List<Integer> chunkExist(String uploadId) {
        throw new BusinessException(KitErrorCode.E01001);
    }

    default void uploadChunk(ChunkMultipartDTO form) {
        throw new BusinessException(KitErrorCode.E01001);
    }

}
