package com.example.honorcitizen.domain.board.dto;

import lombok.Getter;

@Getter
public class BoardAttachmentResponse {

    private final Long id;
    private final String originalFileName;
    private final String url;

    private BoardAttachmentResponse(Long id, String originalFileName, String url) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.url = url;
    }

    public static BoardAttachmentResponse of(Long attachmentId, String originalFileName, String url) {
        return new BoardAttachmentResponse(attachmentId, originalFileName, url);
    }
}
