package org.quyq.gwsu.kit.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.config.properties.FileUploadInfoProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.file.domain.KitFileChunkInfo;
import org.quyq.gwsu.kit.file.domain.KitFileMetaInfo;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2024/5/25
 * @description oss文件上传
 */
@Slf4j
@Setter
public class OssFileServiceImpl extends AbstractFileService{

    public OssFileServiceImpl(FileUploadInfoProperties uploadProperties ,ObjectMapper objectMapper) {
        super(FileServiceType.OSS, uploadProperties.getGroup());
        this.properties = uploadProperties;
        this.objectMapper = objectMapper;
        this.ossClient = buildOssClient();
    }

    private final ObjectMapper objectMapper;
    private final FileUploadInfoProperties properties;

   private final OSS ossClient;

    private OSS buildOssClient(){
        FileUploadInfoProperties.Oss ossProperties = properties.getOss();
        return new OSSClientBuilder().build(ossProperties.getEndpoint(), ossProperties.getAccessKey(),
                ossProperties.getSecretKey());
    }

    private void checkBucketName(OSS ossClient,String bucketName) {
        boolean exist = ossClient.doesBucketExist(bucketName);
        if (!exist) {
            CreateBucketRequest request = new CreateBucketRequest(bucketName);
            request.setCannedACL(CannedAccessControlList.PublicRead);
            ossClient.createBucket(request);
        }
    }

    @Override
    void toUpload(KitFileMetaInfo metaInfo, MultipartFile file) {
        try {
            checkBucketName(ossClient,metaInfo.getFileGroup());

            ossClient.putObject(metaInfo.getFileGroup(), metaInfo.getFileUrl(), file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(KitErrorCode.E01002,e);
        }
    }

    @Override
    byte[] toDownloadBytes(KitFileInfoVO file) throws IOException {
        OSSObject object = ossClient.getObject(file.getFileGroup(), file.getFileUrl());
        return IOUtils.toByteArray(object.getObjectContent());
    }

    @Override
    byte[] toDownloadBytes(KitFileInfoVO file, long startIndex, long endIndex) throws IOException {
        GetObjectRequest request = new GetObjectRequest(file.getFileGroup(), file.getFileUrl());
        request.setRange(startIndex, endIndex);
        OSSObject object = ossClient.getObject(request);
        return IOUtils.toByteArray(object.getObjectContent());
    }

    @Override
    void toDelete(KitFileInfoVO file) {
        ossClient.deleteObject(file.getFileGroup(), file.getFileUrl());
    }

    @Override
    boolean fileIsExist(String group, String fileUrl) {
        if(!ossClient.doesBucketExist(group)){
            return false;
        }
        return ossClient.doesObjectExist(group , fileUrl);
    }

    @Override
    protected String getMultipartUploadId(ChunkMultipartDTO form, String url) {
        checkBucketName(ossClient,group);

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(group, url);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream");
        request.setObjectMetadata(metadata);
        InitiateMultipartUploadResult response = ossClient.initiateMultipartUpload(request);

        return response.getUploadId();
    }


    @Override
    protected void completeCancelCallback(String group, String uploadId, String chunkUrl) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(group, chunkUrl, uploadId);
        ossClient.abortMultipartUpload(request);
    }

    @Override
    protected void doCompleteMultipart(List<KitFileChunkInfo> chunkInfos) {
        Optional<KitFileChunkInfo> infoOpt = chunkInfos.stream().findFirst();
        if(infoOpt.isEmpty()){
            return;
        }
        KitFileChunkInfo info = infoOpt.get();

        List<PartETag> eTags = chunkInfos.stream().map(v -> {

            JsonNode tagJson = objectMapper.readTree(v.getNotes());
            return new PartETag(tagJson.get("partNumber").asInt(),
                    tagJson.get("eTag").asString(),
                    tagJson.get("partSize").asLong(),
                    tagJson.get("partCRC").asLong());
        }).collect(Collectors.toList());


        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(info.getChunkGroup(), info.getChunkUrl(), info.getUploadId(), eTags);
        request.setObjectACL(CannedAccessControlList.PublicRead);
        Map<String, String> headers = new HashMap<>();
        headers.put("x-oss-complete-all", "yes");
        request.setHeaders(headers);
        CompleteMultipartUploadResult response = ossClient.completeMultipartUpload(request);
        log.info("文件合并返回结果：key-{},location-{},eTag-{}", response.getKey(), response.getLocation(), response.getETag());
    }

    @Override
    protected void doMultipartChunk(List<KitFileChunkInfo> infos, List<Integer> result) {
        Optional<KitFileChunkInfo> infoOpt = infos.stream().findFirst();
        if(!infoOpt.isPresent()){
            return;
        }
        KitFileChunkInfo info = infoOpt.get();

        List<PartSummary> partSummaries = listMultipart(info.getChunkGroup(),info.getChunkUrl(), info.getUploadId());

        for (PartSummary partSummary : partSummaries) {
            Optional<KitFileChunkInfo> chunkInfo = infos.stream().filter(v -> v.getChunkOffset().equals(partSummary.getPartNumber() - 1))
                    .findFirst();
            if (chunkInfo.isPresent()) {
                KitFileChunkInfo cInfo = chunkInfo.get();
                if (cInfo.getChunkStreamSize().equals((int)partSummary.getSize()))
                    result.add(cInfo.getChunkOffset());
            }
        }
    }

    private List<PartSummary> listMultipart(String bucketName,String fileUrl, String uploadId) {
        ArrayList<PartSummary> vals = new ArrayList<>();

        PartListing partListing;
        ListPartsRequest request = new ListPartsRequest(bucketName, fileUrl, uploadId);
        do {
            partListing = ossClient.listParts(request);
            vals.addAll(partListing.getParts());
        } while (partListing.isTruncated());

        return vals;
    }

    @Override
    protected void doChunkUpload(KitFileChunkInfo info, ChunkMultipartDTO form) {

        try {
            UploadPartRequest request = new UploadPartRequest();
            request.setBucketName(info.getChunkGroup());
            request.setKey(info.getChunkUrl());
            request.setUploadId(info.getUploadId());
            request.setInputStream(form.getFile().getInputStream());
            request.setPartSize(info.getChunkStreamSize());
            //设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，OSS将返回InvalidArgument错误码
            request.setPartNumber(info.getChunkOffset() + 1);
            UploadPartResult response = ossClient.uploadPart(request);
            info.setNotes(objectMapper.writeValueAsString(response.getPartETag()));
        } catch (IOException e) {
            throw new BusinessException(KitErrorCode.E01006,e);
        }
    }
}
