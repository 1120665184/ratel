package org.quyq.gwsu.kit.utils;


import org.quyq.gwsu.kit.api.file.constant.FileConstants;
import org.quyq.gwsu.kit.file.domain.KitFileChunkInfo;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/6/5
 * @description 文件信息整合工具
 */
public class FileInfoIntegrationUtil {


    private FileInfoIntegrationUtil(){}


    /**
     * 生成文件路径
     * @param categorize 类别
     * @param suffix
     * @param uniqueIdentifier
     * @return
     */
    public static String generateFileUrl(String categorize,String suffix ,String uniqueIdentifier){
        LocalDate now = LocalDate.now();
        return new StringBuilder()
                .append(StringUtils.hasText(categorize) ? categorize : "common").append("/")
                .append(now.getYear()).append("/")
                .append(now.getMonthValue()).append("/")
                .append(uniqueIdentifier)
                .append(StringUtils.hasText(suffix) ? String.format(".%s", suffix) : "").toString();
    }


    /**
     * 拼接前端分段上传数据
     * @param chunks
     * @param result
     */
    public static void addChunkInfoToResult(List<KitFileChunkInfo> chunks , Map<String, Object> result){
        result.put(FileConstants.UPLOAD_ID, chunks.getFirst().getUploadId());
        chunks.forEach(v -> result.put(FileConstants.CHUNK_PREFIX + v.getChunkOffset(),
                getViewChunkUrl(v.getUniqueId(), v.getChunkOffset(), v.getUploadId(),v.getNotes())));
    }

    /**
     * 拼接前端分片上传路径
     * @param uniqueId
     * @param offset
     * @param uploadId
     * @param other
     * @return
     */
    public static String getViewChunkUrl(String uniqueId , Integer offset , String uploadId,String other){
        return String.format("/file/chunk?uniqueIdentifier=%s&offset=%d&uploadId=%s%s", uniqueId, offset, uploadId,
                !StringUtils.hasText(other) ? "" :"&notes="+ new String(Base64.getEncoder().encode(other.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8));
    }

}
