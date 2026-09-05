package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.event.dto.EventAdminDetailResponse;
import com.example.honorcitizen.domain.event.dto.EventAdminListItemResponse;
import com.example.honorcitizen.domain.event.dto.EventCreateRequest;
import com.example.honorcitizen.domain.event.dto.EventCreateResponse;
import com.example.honorcitizen.domain.event.dto.EventUpdateRequest;
import com.example.honorcitizen.domain.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 관리자 전용 CRUD+조회: SecurityConfig의 라우트 검증 후 Service가 관리자 권한을 다시 확인한다.
@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class EventAdminController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventAdminListItemResponse>>> list(
            @AuthenticationPrincipal Long adminId,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(eventService.listForAdmin(adminId, type, visible, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventAdminDetailResponse>> detail(
            @AuthenticationPrincipal Long adminId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.detailForAdmin(adminId, id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventCreateResponse>> create(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestPart("request") EventCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(eventService.create(adminId, request, thumbnail, logo, images)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long id,
            @Valid @RequestPart("request") EventUpdateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        eventService.update(adminId, id, request, thumbnail, logo, images);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long adminId, @PathVariable Long id) {
        eventService.delete(adminId, id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
