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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 관리자 전용 CRUD(api.md §API 3/4/5) — SecurityConfig의 `/api/admin/**`가 ADMIN 역할만 통과시키므로
// 여기서는 별도로 권한을 재확인하지 않는다(라우트 레벨 강제로 충분, api.md "관리자 권한 강제 방식" 참고).
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
            @PathVariable Long id,
            @Valid @RequestBody BoardUpdateRequest request) {
        boardService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        boardService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
