package org.quyq.gwsu.kit.file.service.impl;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/5/25
 * @description cos
 */
@Slf4j
@Setter
public class CosFileServiceImpl extends AbstractFileService{
    public CosFileServiceImpl(FileUploadInfoProperties properties , ObjectMapper objectMapper) {
        super(FileServiceType.COS, properties.getGroup());
        this.objectMapper = objectMapper;
        this.properties = properties;
        client = buildClient();
    }

    private final FileUploadInfoProperties properties;
    private final ObjectMapper objectMapper;

    private final AmazonS3 client;

    private AmazonS3 buildClient(){
        FileUploadInfoProperties.Cos cos = properties.getCos();

        BasicAWSCredentials credential = new BasicAWSCredentials(cos.getAccessKey(), cos.getSecretKey());
        AWSStaticCredentialsProvider provider = new AWSStaticCredentialsProvider(credential);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(cos.getEndpoint(), cos.getRegion());
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(provider)
                .withClientConfiguration(clientConfiguration.withProtocol(Protocol.HTTP))
                .build();
    }

    @Override
    void toUpload(KitFileMetaInfo metaInfo, MultipartFile file) {
        try {
            checkBucket(metaInfo.getFileGroup());

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            PutObjectRequest request = new PutObjectRequest(metaInfo.getFileGroup(), metaInfo.getFileUrl(), file.getInputStream(), metadata);
            request.setCannedAcl(CannedAccessControlList.PublicRead);

            client.putObject(request);
        } catch (IOException e) {
            throw new BusinessException(KitErrorCode.E01002,e);
        }
    }

    @Override
    byte[] toDownloadBytes(KitFileInfoVO file) throws IOException {
        S3Object object = client.getObject(file.getFileGroup(), file.getFileUrl());
        return IOUtils.toByteArray(object.getObjectContent());
    }

    @Override
    byte[] toDownloadBytes(KitFileInfoVO file, long startIndex, long endIndex) throws IOException {
        GetObjectRequest request = new GetObjectRequest(file.getFileGroup(), file.getFileUrl());
        request.setRange(startIndex, endIndex);
        S3Object object = client.getObject(request);
        return IOUtils.toByteArray(object.getObjectContent());
    }

    @Override
    void toDelete(KitFileInfoVO file) {
        client.deleteObject(file.getFileGroup(), file.getFileUrl());
    }

    @Override
    boolean fileIsExist(String group, String fileUrl) {

        if(!client.doesBucketExistV2(group)){
            return false;
        }

        return client.doesObjectExist(group , fileUrl);
    }

    void checkBucket(String group){
        //不存在桶时创建
        boolean exist = client.doesBucketExistV2(group);
        if (!exist) {
            client.createBucket(group);
        }
    }


    @Override
    protected String getMultipartUploadId(ChunkMultipartDTO form, String url) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(group, url);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream");
        request.setCannedACL(CannedAccessControlList.PublicRead);
        InitiateMultipartUploadResult response = client.initiateMultipartUpload(request);

        return response.getUploadId();
    }

    @Override
    protected void completeCancelCallback(String group, String uploadId, String chunkUrl) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(group, chunkUrl, uploadId);
        client.abortMultipartUpload(request);
    }

    @Override
    protected void doCompleteMultipart(List<KitFileChunkInfo> chunkInfos) {
        KitFileChunkInfo info = chunkInfos.getFirst();
        String group = info.getChunkGroup();
        String objectName = info.getChunkUrl();
        String uploadId = info.getUploadId();

        List<PartETag> eTags = chunkInfos.stream().map(v -> {
            JsonNode tagJson = objectMapper.readTree(v.getNotes());

            return new PartETag(tagJson.get("partNumber").asInt(),
                    tagJson.get("eTag").asString());
        }).collect(Collectors.toList());

        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(group, objectName, uploadId, eTags);
        CompleteMultipartUploadResult response = client.completeMultipartUpload(request);
        log.debug("文件合并返回结果：key-{},location-{},eTag-{}", response.getKey(), response.getLocation(), response.getETag());
    }

    @Override
    protected void doMultipartChunk(List<KitFileChunkInfo> infos, List<Integer> result) {
        Optional<KitFileChunkInfo> infoOpt = infos.stream().findFirst();
        if(infoOpt.isEmpty()){
            return;
        }
        KitFileChunkInfo info = infoOpt.get();
        List<PartSummary> partSummaries = listMultipart(info.getChunkGroup(),info.getChunkUrl(), info.getUploadId());


        for (PartSummary partSummary : partSummaries) {
            infos.stream()
                    .filter(v -> v.getChunkOffset().equals(partSummary.getPartNumber() - 1))
                    .findFirst().ifPresent(cInfo -> {
                        if (cInfo.getChunkStreamSize().equals((int) partSummary.getSize()))
                            result.add(cInfo.getChunkOffset());
                    });
        }
    }

    private List<PartSummary> listMultipart(String group, String fileUrl, String uploadId) {
        ArrayList<PartSummary> vals = new ArrayList<>();

        PartListing partListing;
        ListPartsRequest request = new ListPartsRequest(group, fileUrl, uploadId);
        do {
            partListing = client.listParts(request);
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
            request.setPartNumber(info.getChunkOffset() + 1);
            request.setPartSize(info.getChunkStreamSize());
            UploadPartResult result = client.uploadPart(request);
            info.setNotes(objectMapper.writeValueAsString(result.getPartETag()));
        } catch (IOException e) {
            throw new BusinessException(KitErrorCode.E01006,e);
        }
    }
}
