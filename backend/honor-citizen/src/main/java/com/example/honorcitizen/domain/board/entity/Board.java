package com.example.honorcitizen.domain.board.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.BoardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 공지사항/FAQ 등 관리자 작성 텍스트형 게시판. BoardType enum 하나로 통합 관리 —
// 신규 게시판 종류가 생기면 enum 값만 추가한다(테이블 추가 없음). arch.md §5.1 원칙에 따라
// created_by_user_id는 User와 JPA 연관관계 없이 Long으로만 참조한다(Application.userId와 동일 방식).
@Entity
@Table(name = "boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardType boardType;

    // FAQ는 질문(Q)을 이 필드에 저장한다.
    @Column(nullable = false)
    private String title;

    // FAQ는 답변(A)을 이 필드에 저장한다.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 작성 관리자 User.id. 공개 응답에는 노출하지 않는다(순수 내부 감사용, data-model.md §5.4).
    @Column(nullable = false)
    private Long createdByUserId;

    public static Board create(BoardType boardType, String title, String content, Long createdByUserId) {
        Board board = new Board();
        board.boardType = boardType;
        board.title = title;
        board.content = content;
        board.createdByUserId = createdByUserId;
        return board;
    }

    // 전체 재제출(Review PATCH와 동일 원칙) — createdByUserId는 원작성자 감사 추적을 위해 수정 시에도 유지한다.
    public void update(BoardType boardType, String title, String content) {
        this.boardType = boardType;
        this.title = title;
        this.content = content;
    }
}
