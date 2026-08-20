package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// email은 OAuth 계정 식별값이라 이 API로 수정할 수 없다(필드 자체가 없음).
// 확정 정책(2026-08-08, 2026-08-20 재확인): 수정 가능한 필드는 이름·전화번호뿐이며 address는 이 API로 수정하지 않는다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(max = 255)
    private String name;

    @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;
}
