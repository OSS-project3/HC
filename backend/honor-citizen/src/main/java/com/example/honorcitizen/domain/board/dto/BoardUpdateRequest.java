package com.example.honorcitizen.domain.board.dto;

import com.example.honorcitizen.common.enums.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// Review PATCH와 동일 원칙 — 전체 재제출(부분수정 아님).
// keepAttachmentIds도 이 원칙을 따른다: 생략하거나 빈 배열이면 기존 첨부파일을 전부 제거한다는 뜻이다
// (부분 수정이 아니므로 "이 목록에 없는 건 그대로 유지"가 아니라 "이 목록만 유지"가 계약이다).
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateRequest {

    @NotNull
    private BoardType boardType;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    // 유지할 기존 BoardAttachment id 목록. 여기 없는 기존 첨부파일은 삭제된다.
    private List<Long> keepAttachmentIds;
}
