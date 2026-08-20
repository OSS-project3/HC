package com.example.honorcitizen.domain.event.repository;

import com.example.honorcitizen.domain.event.entity.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventImageRepository extends JpaRepository<EventImage, Long> {

    List<EventImage> findByEventPostIdOrderByDisplayOrderAsc(Long eventPostId);

    void deleteByEventPostId(Long eventPostId);

    // keepImageIds 소유권 검증(api.md §API 4) — 반환된 개수가 요청한 id 개수보다 적으면 타 Event
    // 소유이거나 존재하지 않는 id가 섞여 있다는 뜻이다.
    List<EventImage> findByIdInAndEventPostId(List<Long> ids, Long eventPostId);
}
