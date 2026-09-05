package com.example.honorcitizen.domain.school.repository;

import com.example.honorcitizen.domain.school.entity.School;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SchoolRepository extends JpaRepository<School, Long> {

    // 학교명 부분일치 검색. 정확히 일치 > 검색어로 시작 > 그 외 포함 순으로 우선 노출하고, 같은
    // 우선순위 안에서는 이름순이다. 결과 개수는 호출부가 넘기는 Pageable로 제한한다(Page<T>가 아니라
    // List<T>를 반환 — 이 API는 무한스크롤/페이지네이션 UI가 없는 단순 autocomplete이기 때문).
    @Query("""
            SELECT s FROM School s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY
                CASE
                    WHEN LOWER(s.name) = LOWER(:query) THEN 0
                    WHEN LOWER(s.name) LIKE LOWER(CONCAT(:query, '%')) THEN 1
                    ELSE 2
                END,
                s.name ASC
            """)
    List<School> searchByName(@Param("query") String query, Pageable pageable);
}
