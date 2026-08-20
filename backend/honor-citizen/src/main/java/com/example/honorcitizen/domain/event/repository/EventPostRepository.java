package com.example.honorcitizen.domain.event.repository;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.domain.event.entity.EventPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventPostRepository extends JpaRepository<EventPost, Long> {

    // 정렬 기준(data-model.md §1): display_order ASC(NULL은 맨 뒤) → event_date DESC(NULL은 맨 뒤) → created_at DESC.
    // 고정 정렬이라 Pageable에는 정렬 없이 페이지 범위만 넘긴다(Service에서 Sort.unsorted()로 생성).
    @Query("""
            SELECT e FROM EventPost e
            WHERE e.eventType = :eventType AND e.visible = true
            ORDER BY e.displayOrder ASC NULLS LAST, e.eventDate DESC NULLS LAST, e.createdAt DESC
            """)
    Page<EventPost> findVisibleByEventType(@Param("eventType") EventType eventType, Pageable pageable);

    Optional<EventPost> findByIdAndVisibleTrue(Long id);

    // 관리자 목록(api.md API 6) — type/visible 둘 다 선택 필터, 생략하면 해당 조건은 전체.
    @Query("""
            SELECT e FROM EventPost e
            WHERE (:eventType IS NULL OR e.eventType = :eventType)
              AND (:visible IS NULL OR e.visible = :visible)
            ORDER BY e.displayOrder ASC NULLS LAST, e.eventDate DESC NULLS LAST, e.createdAt DESC
            """)
    Page<EventPost> findAllForAdmin(@Param("eventType") EventType eventType, @Param("visible") Boolean visible,
            Pageable pageable);
}
