package org.quyq.gwsu.kit.file.service.impl.client;

import cn.hutool.core.lang.Assert;
import com.google.common.collect.Multimap;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Part;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public class PearlMinioClient {

    private final MinioClient syncClient;
    private final MultipartClient multipartClient;

    public PearlMinioClient(MinioClient syncClient, MinioAsyncClient asyncClient) {
        this.syncClient = syncClient;
        this.multipartClient = new MultipartClient(asyncClient);
    }

    public ObjectWriteResponse putObject(PutObjectArgs args)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        return syncClient.putObject(args);
    }

    public GetObjectResponse getObject(GetObjectArgs args)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        return syncClient.getObject(args);
    }

    public void removeObject(RemoveObjectArgs args)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        syncClient.removeObject(args);
    }

    public boolean bucketExists(BucketExistsArgs args)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        return syncClient.bucketExists(args);
    }

    public void makeBucket(MakeBucketArgs args)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        syncClient.makeBucket(args);
    }

    public StatObjectResponse statObject(StatObjectArgs args)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        return syncClient.statObject(args);
    }

    public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args)
            throws ErrorResponseException, InsufficientDataException, InternalException,
            InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        return syncClient.getPresignedObjectUrl(args);
    }

    public CreateMultipartUploadResponse createMultipartUpload(
            String bucketName, String region, String objectName,
            Multimap<String, String> headers, Multimap<String, String> extraQueryParams)
            throws ServerException, InsufficientDataException, ErrorResponseException,
            NoSuchAlgorithmException, IOException, InvalidKeyException, XmlParserException,
            InvalidResponseException, InternalException {
        return multipartClient.doCreateMultipartUpload(bucketName, region, objectName, headers, extraQueryParams);
    }

    public AbortMultipartUploadResponse abortMultipartUpload(
            String bucketName, String region, String objectName, String uploadId,
            Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException,
            InvalidKeyException, ServerException, XmlParserException, ErrorResponseException,
            InternalException, InvalidResponseException {
        return multipartClient.doAbortMultipartUpload(bucketName, region, objectName, uploadId, extraHeaders, extraQueryParams);
    }

    public ObjectWriteResponse completeMultipartUpload(
            String bucketName, String region, String objectName, String uploadId,
            Part[] parts, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException,
            InvalidKeyException, ServerException, XmlParserException, ErrorResponseException,
            InternalException, InvalidResponseException {
        return multipartClient.doCompleteMultipartUpload(bucketName, region, objectName, uploadId, parts, extraHeaders, extraQueryParams);
    }

    public ListPartsResponse listMultipart(RequestParam param,
            Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams)
            throws NoSuchAlgorithmException, InsufficientDataException, IOException,
            InvalidKeyException, ServerException, XmlParserException, ErrorResponseException,
            InternalException, InvalidResponseException {
        Assert.notNull(param);
        return multipartClient.doListParts(param.getBucketName(), param.getRegion(), param.getObjectName(),
                param.getMaxParts(), param.getPartNumberMarker(), param.getUploadId(), extraHeaders, extraQueryParams);
    }

    private static class MultipartClient extends MinioAsyncClient {
        MultipartClient(MinioAsyncClient client) {
            super(client);
        }

        CreateMultipartUploadResponse doCreateMultipartUpload(
                String bucketName, String region, String objectName,
                Multimap<String, String> headers, Multimap<String, String> extraQueryParams)
                throws ServerException, InsufficientDataException, ErrorResponseException,
                NoSuchAlgorithmException, IOException, InvalidKeyException, XmlParserException,
                InvalidResponseException, InternalException {
            try {
                return createMultipartUploadAsync(bucketName, region, objectName, headers, extraQueryParams).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throwEncapsulatedException(e);
                return null;
            }
        }

        AbortMultipartUploadResponse doAbortMultipartUpload(
                String bucketName, String region, String objectName, String uploadId,
                Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams)
                throws NoSuchAlgorithmException, InsufficientDataException, IOException,
                InvalidKeyException, ServerException, XmlParserException, ErrorResponseException,
                InternalException, InvalidResponseException {
            try {
                return abortMultipartUploadAsync(bucketName, region, objectName, uploadId, extraHeaders, extraQueryParams).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throwEncapsulatedException(e);
                return null;
            }
        }

        ObjectWriteResponse doCompleteMultipartUpload(
                String bucketName, String region, String objectName, String uploadId,
                Part[] parts, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams)
                throws NoSuchAlgorithmException, InsufficientDataException, IOException,
                InvalidKeyException, ServerException, XmlParserException, ErrorResponseException,
                InternalException, InvalidResponseException {
            try {
                return completeMultipartUploadAsync(bucketName, region, objectName, uploadId, parts, extraHeaders, extraQueryParams).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throwEncapsulatedException(e);
                return null;
            }
        }

        ListPartsResponse doListParts(
                String bucketName, String region, String objectName,
                Integer maxParts, Integer partNumberMarker, String uploadId,
                Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams)
                throws NoSuchAlgorithmException, InsufficientDataException, IOException,
                InvalidKeyException, ServerException, XmlParserException, ErrorResponseException,
                InternalException, InvalidResponseException {
            try {
                return listPartsAsync(bucketName, region, objectName, maxParts, partNumberMarker, uploadId, extraHeaders, extraQueryParams).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throwEncapsulatedException(e);
                return null;
            }
        }
    }

    @Data
    @Accessors(chain = true)
    public static class RequestParam {
        private String bucketName;
        private String region;
        private String objectName;
        private Integer maxParts;
        private Integer partNumberMarker;
        private String uploadId;
    }
}
