package com.example.honorcitizen.domain.bulk.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class BulkApplicationCreateResponse {

    private final Long bulkOrderId;
    private final int successCount;
    private final int failedCount;
    private final List<FailedRow> failures;

    private BulkApplicationCreateResponse(Long bulkOrderId, int successCount,
                                           int failedCount, List<FailedRow> failures) {
        this.bulkOrderId = bulkOrderId;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.failures = failures;
    }

    public static BulkApplicationCreateResponse of(Long bulkOrderId, int successCount,
                                                    List<FailedRow> failures) {
        return new BulkApplicationCreateResponse(bulkOrderId, successCount,
                failures.size(), List.copyOf(failures));
    }

    public record FailedRow(int rowNumber, String nameEn, String reason) {}
}
