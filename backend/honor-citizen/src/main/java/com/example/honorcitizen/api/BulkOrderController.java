package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.bulk.dto.BulkApplicationCreateResponse;
import com.example.honorcitizen.domain.bulk.dto.BulkDownloadResponse;
import com.example.honorcitizen.domain.bulk.service.BulkApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BulkOrderController {

    private final BulkApplicationService bulkApplicationService;

    @PostMapping("/applications/bulk")
    public ResponseEntity<ApiResponse<BulkApplicationCreateResponse>> createBulk(
            @AuthenticationPrincipal Long userId,
            @RequestPart("file") MultipartFile zipFile) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(bulkApplicationService.createBulk(userId, zipFile)));
    }

    @GetMapping("/my/bulk-orders/{bulkOrderId}/cards/download")
    public ResponseEntity<ApiResponse<BulkDownloadResponse>> getBulkDownload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long bulkOrderId) {
        return ResponseEntity.ok(ApiResponse.success(
                bulkApplicationService.getBulkDownloadUrl(userId, bulkOrderId)));
    }
}
