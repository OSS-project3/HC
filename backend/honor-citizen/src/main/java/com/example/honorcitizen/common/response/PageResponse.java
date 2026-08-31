package com.example.honorcitizen.common.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// 이 프로젝트 첫 페이징 API(Review 목록조회)를 위한 공용 응답 포맷.
// ApiResponse<PageResponse<T>>로 감싸 기존 공통 응답 규칙을 그대로 유지한다.
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    // Entity 페이지를 응답 DTO 페이지로 변환할 때 사용 — Entity를 API 응답으로 직접 노출하지 않기 위함.
    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    // 페이징 메타는 그대로 두고 content만 바꾼 사본 — 영어 번역 응답(EnglishResponseTranslator)용.
    public <R> PageResponse<R> withContent(List<R> newContent) {
        return new PageResponse<>(newContent, page, size, totalElements, totalPages);
    }
}
