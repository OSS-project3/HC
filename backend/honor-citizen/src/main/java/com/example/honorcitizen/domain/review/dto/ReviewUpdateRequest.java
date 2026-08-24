package com.example.honorcitizen.domain.review.dto;

import com.example.honorcitizen.common.enums.ApplicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotNull
    private ApplicationType applicationType;

    @NotNull
    private Long cardTypeId;

    @NotBlank
    @Size(max = 50)
    private String authorName;

    @NotBlank
    private String content;

    // 프론트에는 없는 필드 — 멀티파트에서 "새 파일 파트 없음"만으로는 "기존 사진 유지"와 "삭제"를
    // 구분할 수 없어 API에서 별도로 받는다(api.md §API 5). keepImageIds를 보내면 keepImageIds가 우선이며
    // 이 필드는 무시된다(단일 이미지 시절의 레거시 클라이언트 호환용).
    private boolean removeImage;

    // 다중 이미지 편집: 유지할 기존 이미지 id 목록(원하는 순서대로). null=기존 전체 유지, 빈 배열=전체 삭제,
    // 그 외=지정 id만 그 순서로 유지. 새 파일들은 유지분 뒤에 전송 순서대로 추가된다(EventUpdateRequest와 동일).
    private List<Long> keepImageIds;
}
