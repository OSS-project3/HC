package com.example.honorcitizen.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String upload(String key, MultipartFile file);

    String uploadBytes(String key, byte[] bytes, String contentType);

    byte[] download(String key);

    String generatePresignedUrl(String key, long expirySeconds);

    void delete(String key);
}
