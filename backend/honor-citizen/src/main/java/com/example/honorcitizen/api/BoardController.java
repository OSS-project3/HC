package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.BoardSearchType;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 공개 조회 전용(api.md §API 1/2) — 비로그인 포함 누구나 접근 가능. 관리자 CRUD는 BoardAdminController 참고.
// Accept-Language: en이면 title/content를 영어로 번역해 응답한다(EnglishResponseTranslator).
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final EnglishResponseTranslator englishResponseTranslator;

    // searchType/keyword — 공지·FAQ 서버 검색(2026-09-05 신규, ReviewController와 동일 계약). keyword
    // 없으면 searchType과 무관하게 기존과 동일하게 동작한다. NOTICE/FAQ 통합검색은 지원하지 않으므로
    // type은 계속 필수다(BoardService.list()가 강제).
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BoardListItemResponse>>> list(
            @RequestParam(required = false) BoardType type,
            @RequestParam(required = false) BoardSearchType searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(ApiResponse.success(englishResponseTranslator.translateBoards(
                boardService.list(type, searchType, keyword, page, size), acceptLanguage)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> detail(
            @PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(ApiResponse.success(
                englishResponseTranslator.translateBoard(boardService.detail(id), acceptLanguage)));
    }
}
