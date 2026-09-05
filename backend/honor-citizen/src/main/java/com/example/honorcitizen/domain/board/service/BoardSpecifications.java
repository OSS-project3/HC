package com.example.honorcitizen.domain.board.service;

import com.example.honorcitizen.common.enums.BoardSearchType;
import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.domain.board.entity.Board;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

// GET /api/boards의 동적 필터(type+searchType/keyword)를 JpaSpecificationExecutor로 구현
// (ReviewSpecifications와 동일 패턴 — 2026-09-05, 공지 서버검색 신규). 각 메서드는 조건이 없으면
// cb.conjunction()(항상 참)을 반환한다 — 이 프로젝트의 Spring Data JPA 버전은 Specification에
// null을 넘기면 예외를 던지므로 null을 반환하면 안 된다(ReviewSpecifications와 동일 주의사항).
final class BoardSpecifications {

    private BoardSpecifications() {
    }

    // 정책(2026-09-05): NOTICE/FAQ 통합검색은 지원하지 않는다 — type은 항상 지정해서 그 안에서만 검색한다.
    // 그래도 이 메서드 자체는 null-safe하게 만들어 Review의 cardTypeId(Long, optional)와 같은 형태를
    // 유지한다(컨트롤러의 @RequestParam이 실제로 type을 필수로 강제한다, BoardService.list() 참고).
    static Specification<Board> boardType(BoardType type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction() : cb.equal(root.get("boardType"), type);
    }

    // 검색 계약(ReviewSpecifications.keyword와 동일): keyword 없으면 searchType과 무관하게 조건을
    // 걸지 않는다. keyword 있는데 searchType 없으면 ALL로 처리한다. Board는 title(공지 제목/FAQ 질문),
    // content(공지 본문/FAQ 답변) 둘뿐이라 Review의 AUTHOR에 대응하는 항목이 없다.
    static Specification<Board> keyword(BoardSearchType searchType, String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            BoardSearchType type = searchType == null ? BoardSearchType.ALL : searchType;
            String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";

            return switch (type) {
                case TITLE -> cb.like(cb.lower(root.get("title")), pattern);
                case CONTENT -> cb.like(cb.lower(root.get("content")), pattern);
                case ALL -> cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern));
            };
        };
    }
}
