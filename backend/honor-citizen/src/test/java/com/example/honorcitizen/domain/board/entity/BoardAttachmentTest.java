package com.example.honorcitizen.domain.board.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardAttachmentTest {

    @Test
    void createSetsAllFields() {
        BoardAttachment attachment = BoardAttachment.create(1L, 2L, 0);

        assertThat(attachment.getBoardId()).isEqualTo(1L);
        assertThat(attachment.getUploadFileId()).isEqualTo(2L);
        assertThat(attachment.getDisplayOrder()).isEqualTo(0);
    }
}
