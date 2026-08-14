package com.example.honorcitizen.domain.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Board:UploadFile = 1:N join 엔티티 — UploadFile은 아무것도 참조하지 않는 공용 메타데이터 테이블이라는
// 원칙(docs/api/upload-file.md) 때문에 직접 1:N을 걸지 않는다. NOTICE 전용, FAQ는 사용하지 않는다.
@Entity
@Table(name = "board_attachments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"board_id", "upload_file_id"}),
        @UniqueConstraint(columnNames = {"board_id", "display_order"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "upload_file_id", nullable = false)
    private Long uploadFileId;

    // 0부터 시작하는 노출 순서 — 첨부 전송 순서대로 채운다.
    @Column(nullable = false)
    private int displayOrder;

    public static BoardAttachment create(Long boardId, Long uploadFileId, int displayOrder) {
        BoardAttachment attachment = new BoardAttachment();
        attachment.boardId = boardId;
        attachment.uploadFileId = uploadFileId;
        attachment.displayOrder = displayOrder;
        return attachment;
    }
}
