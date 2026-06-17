package org.quyq.gwsu.kit.api.file;


import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.dto.FileUploadDTO;
import org.quyq.gwsu.kit.api.file.fallback.FileClientApiFallbackFactory;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/6/6
 * @description 文件上传有现成工具类提供，无需使用该api ，见{@link org.quyq.gwsu.kit.api.utils.FileUtils}
 *
 */
@ApiClient(value = CoreConstants.Server.KIT_NAME, note = "文件上传API", fallbackFactory = FileClientApiFallbackFactory.class)
@HttpExchange("/file")
public interface FileClientApi {


    /**
     * 简单文件上传
     *
     * @param form
     * @return
     */
    @PostExchange(value = "upload", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<KitFileInfoVO> uploadSingle(@ModelAttribute FileUploadDTO form);


    /**
     * 删除文件
     *
     * @param fileId
     * @return
     */
    @PostExchange("remove/{fileId}")
    R<Void> removeByFileId(@PathVariable String fileId);


    /**
     * 返回分片上传需要的签名数据URL及 uploadId
     *
     * @param form
     * @return
     */
    @PostExchange(value = "/createMultipartUpload", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<Map<String, Object>> createMultipartUpload(@ModelAttribute ChunkMultipartDTO form);


    /**
     * 分片上传完后合并
     *
     * @param form
     * @return /
     */
    @PostExchange("/completeMultipartUpload")
    R<KitFileInfoVO> completeMultipartUpload(@RequestBody ChunkMultipartDTO form);


    /**
     * 分片上传：返回已上传的分片
     *
     * @param form
     * @return
     */
    @GetExchange("/chunk")
    R<List<Integer>> chunkExist(ChunkMultipartDTO form);

    /**
     * 上传指定chunk
     *
     * @param form
     * @return
     * @throws Exception
     */
    @PostExchange(value = "/chunk", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<Void> chunkUpload(@ModelAttribute ChunkMultipartDTO form);


    /**
     * 下载文件（支持 Range 分片下载）
     *
     * @param fileId 文件ID
     * @param range  HTTP Range 头，如 "bytes=0-10485759"，为 null 时不分片
     * @return 文件内容字节
     */
    @GetExchange("download/{fileId}")
    byte[] download(@PathVariable String fileId, @RequestHeader(value = "Range", required = false) String range);

    /**
     * 获取文件信息
     *
     * @param fileId
     * @return
     */
    @PostExchange("info/{fileId}")
    R<KitFileInfoVO> getFileInfo(@PathVariable String fileId);

}
