package org.quyq.gwsu.kit.api.file.fallback;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.FileClientApi;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.dto.FileUploadDTO;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FileClientApiFallbackFactory implements FallbackFactory<FileClientApi> {
    @Override
    public FileClientApi create(Throwable cause) {
        log.error(cause.getMessage(), cause);
        return new FileClientApi() {

            @Override
            public R<KitFileInfoVO> uploadSingle(FileUploadDTO form) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<Void> removeByFileId(String fileId) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<Map<String, Object>> createMultipartUpload(ChunkMultipartDTO form) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<KitFileInfoVO> completeMultipartUpload(ChunkMultipartDTO form) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<List<Integer>> chunkExist(ChunkMultipartDTO form) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public R<Void> chunkUpload(ChunkMultipartDTO form) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }

            @Override
            public byte[] download(String fileId, String range) {
                throw new BusinessException(cause);
            }

            @Override
            public R<KitFileInfoVO> getFileInfo(String fileId) {
                return R.fail("服务暂时不可用: " + cause.getMessage());
            }
        };
    }
}
