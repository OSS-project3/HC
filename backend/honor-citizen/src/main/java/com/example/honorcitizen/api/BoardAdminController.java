package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.board.dto.BoardCreateRequest;
import com.example.honorcitizen.domain.board.dto.BoardCreateResponse;
import com.example.honorcitizen.domain.board.dto.BoardUpdateRequest;
import com.example.honorcitizen.domain.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 관리자 전용 CRUD: SecurityConfig의 라우트 검증 후 Service가 관리자 권한을 다시 확인한다.
@RestController
@RequestMapping("/api/admin/boards")
@RequiredArgsConstructor
public class BoardAdminController {

    private final BoardService boardService;

    @PostMapping
    public ResponseEntity<ApiResponse<BoardCreateResponse>> create(
            @AuthenticationPrincipal Long adminUserId,
            @Valid @RequestPart("request") BoardCreateRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(boardService.create(adminUserId, request, attachments)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long id,
            @Valid @RequestPart("request") BoardUpdateRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        boardService.update(adminUserId, id, request, attachments);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long adminUserId,
            @PathVariable Long id) {
        boardService.delete(adminUserId, id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
