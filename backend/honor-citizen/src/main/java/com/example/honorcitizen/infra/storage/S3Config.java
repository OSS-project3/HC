package com.example.honorcitizen.infra.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    // 로컬(MinIO)용 선택 설정. 값이 비어 있으면 override하지 않아 운영(실제 AWS S3)에서는 기존 동작 그대로다.
    //  - endpoint          : S3Client(업로드/삭제)가 붙는 내부 주소(도커 네트워크의 http://minio:9000).
    //  - presign-endpoint  : presigned URL에 박힐 외부 주소(브라우저가 접근하는 http://localhost:9000).
    //                        업로드는 컨테이너 내부망으로, URL은 호스트에서 열려야 하므로 둘을 분리한다.
    //  - path-style        : MinIO는 가상 호스팅(bucket.host)이 아닌 path-style(host/bucket)을 요구한다.
    @Bean
    public S3Client s3Client(@Value("${cloud.aws.credentials.access-key}") String accessKey,
                             @Value("${cloud.aws.credentials.secret-key}") String secretKey,
                             @Value("${cloud.aws.s3.region}") String region,
                             @Value("${cloud.aws.s3.endpoint:}") String endpoint,
                             @Value("${cloud.aws.s3.path-style:false}") boolean pathStyle) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(@Value("${cloud.aws.credentials.access-key}") String accessKey,
                                   @Value("${cloud.aws.credentials.secret-key}") String secretKey,
                                   @Value("${cloud.aws.s3.region}") String region,
                                   @Value("${cloud.aws.s3.presign-endpoint:}") String presignEndpoint,
                                   @Value("${cloud.aws.s3.path-style:false}") boolean pathStyle) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        if (!presignEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(presignEndpoint));
        }
        return builder.build();
    }
}
