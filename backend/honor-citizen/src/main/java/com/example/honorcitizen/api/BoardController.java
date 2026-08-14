package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.board.dto.BoardDetailResponse;
import com.example.honorcitizen.domain.board.dto.BoardListItemResponse;
import com.example.honorcitizen.domain.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 공개 조회 전용(api.md §API 1/2) — 비로그인 포함 누구나 접근 가능. 관리자 CRUD는 BoardAdminController 참고.
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BoardListItemResponse>>> list(
            @RequestParam(required = false) BoardType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        return ResponseEntity.ok(ApiResponse.success(boardService.list(type, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(boardService.detail(id)));
    }
}
