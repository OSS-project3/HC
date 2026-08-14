package com.example.honorcitizen.domain.board.repository;

import com.example.honorcitizen.domain.board.entity.BoardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardAttachmentRepository extends JpaRepository<BoardAttachment, Long> {

    List<BoardAttachment> findByBoardIdOrderByDisplayOrderAsc(Long boardId);

    void deleteByBoardId(Long boardId);
}
