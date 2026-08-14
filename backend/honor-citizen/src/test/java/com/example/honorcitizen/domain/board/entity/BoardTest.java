package com.example.honorcitizen.domain.board.entity;

import com.example.honorcitizen.common.enums.BoardType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardTest {

    @Test
    void createSetsAllFields() {
        Board board = Board.create(BoardType.FAQ, "카드 발급까지 얼마나 걸리나요?", "영업일 기준 5~7일 소요됩니다.", 1L);

        assertThat(board.getBoardType()).isEqualTo(BoardType.FAQ);
        assertThat(board.getTitle()).isEqualTo("카드 발급까지 얼마나 걸리나요?");
        assertThat(board.getContent()).isEqualTo("영업일 기준 5~7일 소요됩니다.");
        assertThat(board.getCreatedByUserId()).isEqualTo(1L);
    }

    @Test
    void updateOverwritesBoardTypeTitleAndContentButNotCreator() {
        Board board = Board.create(BoardType.FAQ, "원래 질문", "원래 답변", 1L);

        board.update(BoardType.NOTICE, "새 제목", "새 내용");

        assertThat(board.getBoardType()).isEqualTo(BoardType.NOTICE);
        assertThat(board.getTitle()).isEqualTo("새 제목");
        assertThat(board.getContent()).isEqualTo("새 내용");
        assertThat(board.getCreatedByUserId()).isEqualTo(1L);
    }
}
