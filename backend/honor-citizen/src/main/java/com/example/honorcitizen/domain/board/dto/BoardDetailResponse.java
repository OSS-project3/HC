package com.example.honorcitizen.domain.board.dto;

import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.domain.board.entity.Board;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class BoardDetailResponse {

    private final Long id;
    private final BoardType boardType;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;
    private final List<BoardAttachmentResponse> attachments;
    private final NextBoard next;

    private BoardDetailResponse(Long id, BoardType boardType, String title, String content, LocalDateTime createdAt,
            List<BoardAttachmentResponse> attachments, NextBoard next) {
        this.id = id;
        this.boardType = boardType;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.attachments = attachments;
        this.next = next;
    }

    public static BoardDetailResponse of(Board board, List<BoardAttachmentResponse> attachments, NextBoard next) {
        return new BoardDetailResponse(board.getId(), board.getBoardType(), board.getTitle(), board.getContent(),
                board.getCreatedAt(), attachments, next);
    }

    // 영어 응답용 — 번역된 title/content/next.title로 바꾼 사본. 첨부파일명은 번역하지 않는다.
    public BoardDetailResponse withTranslated(String title, String content, String nextTitle) {
        NextBoard translatedNext = next == null ? null : new NextBoard(next.id, nextTitle);
        return new BoardDetailResponse(id, boardType, title, content, createdAt, attachments, translatedNext);
    }

    @Getter
    public static class NextBoard {
        private final Long id;
        private final String title;

        private NextBoard(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        public static NextBoard from(Board board) {
            return new NextBoard(board.getId(), board.getTitle());
        }
    }
}
