package com.example.honorcitizen.domain.bulk.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BulkDownloadResponse {

    private final Long bulkOrderId;
    private final String downloadUrl;
    private final LocalDateTime expiresAt;
    private final String fileName;
    private final int issuedCount;
    private final int totalCount;

    private BulkDownloadResponse(Long bulkOrderId, String downloadUrl, LocalDateTime expiresAt,
                                  String fileName, int issuedCount, int totalCount) {
        this.bulkOrderId = bulkOrderId;
        this.downloadUrl = downloadUrl;
        this.expiresAt = expiresAt;
        this.fileName = fileName;
        this.issuedCount = issuedCount;
        this.totalCount = totalCount;
    }

    public static BulkDownloadResponse of(Long bulkOrderId, String downloadUrl,
                                          LocalDateTime expiresAt, String fileName,
                                          int issuedCount, int totalCount) {
        return new BulkDownloadResponse(bulkOrderId, downloadUrl, expiresAt,
                fileName, issuedCount, totalCount);
    }
}
