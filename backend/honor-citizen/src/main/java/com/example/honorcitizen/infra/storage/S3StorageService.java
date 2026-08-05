package com.example.honorcitizen.infra.storage;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String baseUrl;

    public S3StorageService(S3Client s3Client,
                            S3Presigner s3Presigner,
                            @Value("${cloud.aws.s3.bucket}") String bucketName,
                            @Value("${cloud.aws.s3.base-url}") String baseUrl) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.baseUrl = baseUrl;
    }

    @Override
    public String upload(String key, MultipartFile file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return baseUrl + "/" + key;
        } catch (IOException e) {
            log.error("S3 upload failed for key: {}", key, e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public String uploadBytes(String key, byte[] bytes, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(bytes));
        return baseUrl + "/" + key;
    }

    @Override
    public byte[] download(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (NoSuchKeyException e) {
            log.error("S3 object not found for key: {}", key);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public String generatePresignedUrl(String key, long expirySeconds) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }
}
