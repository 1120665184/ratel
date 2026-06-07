package org.quyq.gwsu.kit.file.service.impl;

import com.google.common.collect.HashMultimap;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.constant.FileConstants;
import org.quyq.gwsu.kit.api.file.dto.ChunkMultipartDTO;
import org.quyq.gwsu.kit.api.file.enums.FileServiceType;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.config.properties.FileUploadInfoProperties;
import org.quyq.gwsu.kit.errcode.KitErrorCode;
import org.quyq.gwsu.kit.file.domain.KitFileChunkInfo;
import org.quyq.gwsu.kit.file.domain.KitFileMetaInfo;
import org.quyq.gwsu.kit.file.service.impl.client.PearlMinioClient;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/5/24
 * @description minio文件服务
 */
@Slf4j
public class MinioFileServiceImpl extends AbstractFileService {

    public MinioFileServiceImpl(FileUploadInfoProperties uploadProperties) {
        super(FileServiceType.MINIO, uploadProperties.getGroup());
        this.properties = uploadProperties;
        client = buildMinioClient();
    }

    private final FileUploadInfoProperties properties;

    private final PearlMinioClient client;

    private static final Integer EXPIRY_CHUNK = 60 * 60 * 24 * 7;

    private PearlMinioClient buildMinioClient() {
        MinioClient syncClient = MinioClient.builder()
                .endpoint(properties.getMinio().getUrl())
                .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                .build();
        MinioAsyncClient asyncClient = MinioAsyncClient.builder()
                .endpoint(properties.getMinio().getUrl())
                .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                .build();
        return new PearlMinioClient(syncClient, asyncClient);
    }

    @Override
    void toUpload(KitFileMetaInfo metaInfo, MultipartFile file) {
        makeBucket(metaInfo.getFileGroup());


        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(metaInfo.getFileGroup())
                    .object(metaInfo.getFileUrl())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            client.putObject(args);
        } catch (Exception e) {
            throw new BusinessException(KitErrorCode.E01002, e);
        }

    }


    private void makeBucket(String bucketName) {
        try {
            if (!bucketExists(bucketName)) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());

            }
        } catch (Exception e) {
            throw new BusinessException(e);
        }

    }

    /**
     * 判断Bucket是否存在，true：存在，false：不存在
     */
    private boolean bucketExists(String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    @Override
    byte[] toDownloadBytes(KitFileInfoVO file) throws IOException {
        try (GetObjectResponse object = client.getObject(
                GetObjectArgs.builder()
                        .bucket(file.getFileGroup())
                        .object(file.getFileUrl())
                        .build())) {
            return IOUtils.toByteArray(object);
        } catch (ServerException | InsufficientDataException | ErrorResponseException | NoSuchAlgorithmException |
                 InvalidKeyException | InvalidResponseException | XmlParserException | InternalException ex) {
            throw new BusinessException(KitErrorCode.E01003, ex);
        }
    }

    @Override
    byte[] toDownloadBytes(KitFileInfoVO file, long startIndex, long endIndex) throws IOException {
        try (GetObjectResponse object = client.getObject(
                GetObjectArgs.builder()
                        .bucket(file.getFileGroup())
                        .object(file.getFileUrl())
                        .offset(startIndex)
                        .length(endIndex - startIndex + 1)
                        .build())) {
            return IOUtils.toByteArray(object);
        } catch (ServerException | InsufficientDataException | ErrorResponseException | NoSuchAlgorithmException |
                 InvalidKeyException | InvalidResponseException | XmlParserException | InternalException ex) {
            throw new BusinessException(KitErrorCode.E01003, ex);
        }
    }

    @Override
    void toDelete(KitFileInfoVO file) {
        try {
            if (fileIsExist(file.getFileGroup(), file.getFileUrl())) {
                client.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(file.getFileGroup())
                                .object(file.getFileUrl())
                                .build()
                );
            }
        } catch (Exception e) {
            throw new BusinessException(KitErrorCode.E01007, e);
        }
    }

    @Override
    boolean fileIsExist(String group, String fileUrl) {

        try {

            if (!bucketExists(group)) {
                return false;
            }

            StatObjectResponse response = client.statObject(StatObjectArgs.builder().bucket(group).object(fileUrl).build());
            return Objects.nonNull(response);
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    protected String getMultipartUploadId(ChunkMultipartDTO form, String url) {
        String contentType = "application/octet-stream";
        HashMultimap<String, String> headers = HashMultimap.create();
        headers.put("Content-Type", contentType);

        CreateMultipartUploadResponse response;
        try {
            response = client.createMultipartUpload(group, null, url, headers, null);
        } catch (Exception e) {
            throw new BusinessException(e);
        }
        return response.result().uploadId();
    }

    @Override
    protected void multipartUploadCreatedChunkInfoCallback(List<KitFileChunkInfo> chunkInfos) {
        //  请求Minio 服务，获取每个分块带签名的上传URL
        Map<String, String> reqParams = new HashMap<>();

        for (KitFileChunkInfo chunk : chunkInfos) {
            reqParams.put("partNumber", String.valueOf(chunk.getChunkOffset() + 1));
            reqParams.put(FileConstants.UPLOAD_ID, chunk.getUploadId());
            try {
                String uploadUrl = client.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(chunk.getChunkGroup())
                                .object(chunk.getChunkUrl())
                                .expiry(EXPIRY_CHUNK)
                                .extraQueryParams(reqParams)
                                .build());
                chunk.setNotes(uploadUrl);
            } catch (Exception e) {
                throw new BusinessException(e);
            }

        }
    }

    @Override
    protected void completeCancelCallback(String group, String uploadId, String chunkUrl) {
        try {
            client.abortMultipartUpload(group, null, chunkUrl, uploadId, null, null);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected void doCompleteMultipart(List<KitFileChunkInfo> chunkInfos) {
        Optional<KitFileChunkInfo> infoOpt = chunkInfos.stream().findFirst();
        if (!infoOpt.isPresent()) {
            return;
        }

        KitFileChunkInfo info = infoOpt.get();

        try {
            PearlMinioClient.RequestParam param = new PearlMinioClient.RequestParam();
            param.setBucketName(info.getChunkGroup())
                    .setObjectName(info.getChunkUrl())
                    .setMaxParts(1000)
                    .setPartNumberMarker(0)
                    .setUploadId(info.getUploadId());
            ListPartsResponse partResult = client.listMultipart(param, null, null);
            if (!partResult.result().partList().isEmpty()) {
                List<Part> sortedParts = partResult.result().partList().stream()
                        .sorted(Comparator.comparingInt(Part::partNumber))
                        .collect(Collectors.toList());
                Part[] parts = sortedParts.toArray(new Part[0]);
                client.completeMultipartUpload(info.getChunkGroup(), null, info.getChunkUrl(), info.getUploadId(), parts, null, null);

            }
        } catch (Exception e) {
            throw new BusinessException(KitErrorCode.E01005, e);

        }
    }

    @Override
    protected void doMultipartChunk(List<KitFileChunkInfo> infos, List<Integer> result) {
        Optional<KitFileChunkInfo> infoOpt = infos.stream().findFirst();
        if (infoOpt.isEmpty()) {
            return;
        }
        KitFileChunkInfo info = infoOpt.get();

        //找出上传完成的chunk
        ListPartsResponse partResult;

        try {
            final PearlMinioClient.RequestParam param = new PearlMinioClient.RequestParam();
            param.setBucketName(info.getChunkGroup())
                    .setObjectName(info.getChunkUrl())
                    .setMaxParts(1000)
                    .setPartNumberMarker(0)
                    .setUploadId(info.getUploadId());
            partResult = client.listMultipart(param, null, null);
        } catch (Exception e) {
            throw new BusinessException(e);
        }

        List<Part> parts = CollectionUtils.isEmpty(partResult.result().partList()) ? Collections.emptyList() : partResult.result().partList();
        if (CollectionUtils.isEmpty(parts))
            return;

        for (Part part : parts) {
            for (KitFileChunkInfo i : infos) {
                if (part.partNumber() == i.getChunkOffset() + 1) {
                    if (part.partSize() == i.getChunkStreamSize())
                        result.add(i.getChunkOffset());
                    break;
                }
            }
        }
    }

    @Override
    protected void doChunkUpload(KitFileChunkInfo info, ChunkMultipartDTO form) {
        if (StringUtils.hasText(form.getNotes()) && Objects.nonNull(form.getFile())) {
            try {
                sendRequest(new String(Base64.getDecoder().decode(form.getNotes()
                        .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), form.getFile().getInputStream());
            } catch (IOException e) {
                throw new BusinessException(KitErrorCode.E01006, e);
            }
        }
    }

    private void sendRequest(String url, InputStream inputStream) throws IOException {
        HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader("Content-Type", "application/octet-stream");
        httpPut.setEntity(new ByteArrayEntity(IOUtils.toByteArray(inputStream)));

        try (inputStream; CloseableHttpClient httpClient = HttpClients.createDefault()) {
            httpClient.execute(httpPut);
        }


    }


}
