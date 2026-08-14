package com.example.honorcitizen.domain.board.dto;

import com.example.honorcitizen.domain.board.entity.Board;
import lombok.Getter;

@Getter
public class BoardCreateResponse {

    private final Long id;

    private BoardCreateResponse(Long id) {
        this.id = id;
    }

    public static BoardCreateResponse from(Board board) {
        return new BoardCreateResponse(board.getId());
    }
}
