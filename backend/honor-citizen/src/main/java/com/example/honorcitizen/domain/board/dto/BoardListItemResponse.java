package com.example.honorcitizen.domain.board.dto;

import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.domain.board.entity.Board;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BoardListItemResponse {

    private final Long id;
    private final BoardType boardType;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;

    private BoardListItemResponse(Long id, BoardType boardType, String title, String content, LocalDateTime createdAt) {
        this.id = id;
        this.boardType = boardType;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static BoardListItemResponse from(Board board) {
        return new BoardListItemResponse(
                board.getId(), board.getBoardType(), board.getTitle(), board.getContent(), board.getCreatedAt());
    }

    // 영어 응답용 — 번역된 title/content로 바꾼 사본(EnglishResponseTranslator 참고).
    public BoardListItemResponse withTranslated(String title, String content) {
        return new BoardListItemResponse(id, boardType, title, content, createdAt);
    }
}
