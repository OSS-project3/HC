package com.example.honorcitizen.domain.board.repository;

import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.domain.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    Page<Board> findByBoardType(BoardType boardType, Pageable pageable);

    // 단건조회의 "다음글"(더 오래된 글, 같은 boardType 안에서만) 조회 — id 순서가 createdAt 순서와
    // 항상 일치하므로(IDENTITY 자동증가 + 즉시 저장) id 기준 PK 범위 조회만으로 결정론적으로 처리 가능하다.
    Optional<Board> findFirstByIdLessThanAndBoardTypeOrderByIdDesc(Long id, BoardType boardType);
}
