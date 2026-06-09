package org.quyq.gwsu.kit.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.kit.api.file.FileClientApi;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.dto.FileStreamWrapper;
import org.quyq.gwsu.kit.api.file.dto.FileUploadDTO;
import org.quyq.gwsu.kit.api.file.dto.KitFileInfoDTO;
import org.quyq.gwsu.kit.api.file.enums.FileScope;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.file.domain.KitFileChunkInfo;
import org.quyq.gwsu.kit.file.domain.KitFileInfo;
import org.quyq.gwsu.kit.file.domain.KitFileMetaInfo;
import org.quyq.gwsu.kit.file.media.AllowFileType;
import org.quyq.gwsu.kit.file.service.FileServiceManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("file")
@Tag(name = "文件模块")
@TableModelPermission({KitFileInfo.class , KitFileChunkInfo.class , KitFileMetaInfo.class})
@Slf4j
@RequiredArgsConstructor
public class FileController implements FileClientApi {

    private final AllowFileType allowFileType;

    private final FileServiceManager fileServiceManager;

    @PostMapping("page")
    @Operation(summary = "通过条件分页查询")
    public R<IPage<KitFileInfoVO>> pageByCondition(@RequestBody KitFileInfoDTO form) {
        return R.ok(fileServiceManager.get().pageByCondition(form));
    }

    @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LoginAllowAccess
    @Operation(summary = "简单文件上传")
    @Override
    public R<KitFileInfoVO> uploadSingle(@ModelAttribute FileUploadDTO form) {
        allowFileType.valid(form.getFile());
        return R.ok(KitFileInfo.buildVO(fileServiceManager.get().upload(form)));
    }

    @PostMapping("info/{fileId}")
    @Operation(summary = "获取文件信息")
    @Override
    public R<KitFileInfoVO> getFileInfo(@PathVariable String fileId) {
        return R.ok(fileServiceManager.get().getById(fileId));
    }

    @Override
    public byte[] download(String fileId, String range) {
        FileStreamWrapper wrapper = fileServiceManager.get().download(fileId, range);
        return wrapper != null ? wrapper.getData() : null;
    }

    @GetMapping("stream/{fileId}")
    @LoginAllowAccess
    @Operation(summary = "浏览器下载文件")
    public void downloadForBrowser(@PathVariable String fileId,
                                   HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        String range = request.getHeader("Range");
        FileStreamWrapper wrapper = fileServiceManager.get().download(fileId, range);

        if (Objects.isNull(wrapper) || Objects.isNull(wrapper.getData())) {
            response.sendError(404, "资源未找到");
            return;
        }

        response.setContentType(wrapper.getMediaType());
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(wrapper.getFileName(), "UTF-8"));

        if (wrapper.isPartial()) {
            response.setHeader("Accept-Ranges", "bytes");
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setContentLength(wrapper.getData().length);
            response.setHeader("Content-Range",
                    "bytes " + wrapper.getStartIndex() + "-" + wrapper.getEndIndex() + "/" + wrapper.getFileSize());
        } else {
            if (fileServiceManager.get().getById(fileId) != null
                    && FileScope.PUBLIC.equals(fileServiceManager.get().getById(fileId).getScope())) {
                response.setHeader("Cache-Control", "public, max-age=2592000");
            }
        }

        response.getOutputStream().write(wrapper.getData());
        response.getOutputStream().flush();
    }

    @PostMapping("remove/{fileId}")
    @Operation(summary = "删除文件")
    @Override
    public R<Void> removeByFileId(@PathVariable String fileId) {
        fileServiceManager.get().remove(fileId);
        return R.ok();
    }


    /*-----------断点续传----------*/

    @PostMapping(value = "/createMultipartUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LoginAllowAccess
    @Operation(summary = "分片上传：生成分片")
    @Override
    public R<Map<String, Object>> createMultipartUpload(@ModelAttribute ChunkMultipartDTO form) {
        if (Objects.isNull(form.getFile())) {
            return R.fail("请截取头部流上传检验文件类型");
        }
        allowFileType.valid(form.getFile());
        return R.ok(fileServiceManager.get().createMultipartUpload(form));
    }

    @PostMapping("/completeMultipartUpload")
    @LoginAllowAccess
    @Operation(summary = "分片上传：分片合并")
    @Override
    public R<KitFileInfoVO> completeMultipartUpload(@RequestBody ChunkMultipartDTO form) {
        return R.ok(KitFileInfo.buildVO(fileServiceManager.get().completeMultipartUpload(form)));
    }

    @GetMapping("/chunk")
    @LoginAllowAccess
    @Operation(summary = "分片上传：返回已上传的分片")
    @Override
    public R<List<Integer>> chunkExist(ChunkMultipartDTO form) {
        return R.ok(fileServiceManager.get().chunkExist(form.getUploadId()));
    }

    @PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LoginAllowAccess
    @Operation(summary = "分片上传：上传指定分片")
    @Override
    public R<Void> chunkUpload(@ModelAttribute ChunkMultipartDTO form) {
        fileServiceManager.get().uploadChunk(form);
        return R.ok();
    }

}
