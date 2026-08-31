package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.event.dto.EventDetailResponse;
import com.example.honorcitizen.domain.event.dto.EventListItemResponse;
import com.example.honorcitizen.domain.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 공개 조회 전용(api.md §API 1/2) — 비로그인 포함 누구나 접근 가능. 관리자 CRUD는 EventAdminController 참고.
// Accept-Language: en이면 자유 텍스트(title/eventDateText/place/host/content)를 영어로 번역해 응답한다.
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EnglishResponseTranslator englishResponseTranslator;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventListItemResponse>>> list(
            @RequestParam(required = false) EventType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(ApiResponse.success(
                englishResponseTranslator.translateEvents(eventService.list(type, page, size), acceptLanguage)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> detail(
            @PathVariable Long id,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(ApiResponse.success(
                englishResponseTranslator.translateEvent(eventService.detail(id), acceptLanguage)));
    }
}
