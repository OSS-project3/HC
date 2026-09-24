package com.example.honorcitizen.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String upload(String key, MultipartFile file);

    String uploadBytes(String key, byte[] bytes, String contentType);

    byte[] download(String key);

    String generatePresignedUrl(String key, long expirySeconds);

    // 관리자 카드 개별 다운로드 강제(2026-09-24 확정) 전용 — 브라우저가 인라인으로 열지 않고
    // downloadFileName으로 바로 저장하도록 Content-Disposition: attachment를 실어 보낸다.
    // 기존 generatePresignedUrl(인라인 열람용, 다른 여러 곳에서 재사용 중)은 그대로 둔다.
    String generatePresignedDownloadUrl(String key, long expirySeconds, String downloadFileName);

    void delete(String key);
}
