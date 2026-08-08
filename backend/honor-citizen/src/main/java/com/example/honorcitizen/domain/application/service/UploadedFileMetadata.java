package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.UploadFileType;

/**
 * S3 업로드가 끝난 관리 파일의 메타데이터.
 *
 * ApplicationService는 S3 업로드만 수행해 이 값을 만들고,
 * UploadFile DB row 생성은 ApplicationPersistenceService의 트랜잭션 안에서 처리한다.
 */
record UploadedFileMetadata(
        String originalName,
        String storedName,
        String filePath,
        UploadFileType fileType,
        String mimeType,
        long fileSize
) {
}
