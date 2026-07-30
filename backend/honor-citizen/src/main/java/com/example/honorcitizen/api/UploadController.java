package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.photo.dto.PhotoUploadResponse;
import com.example.honorcitizen.domain.photo.service.PhotoUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final PhotoUploadService photoUploadService;

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadPhoto(
            @AuthenticationPrincipal Long userId,
            @RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.ok(ApiResponse.success(photoUploadService.upload(userId, photo)));
    }
}
