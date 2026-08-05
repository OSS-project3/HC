package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    private String name;

    @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;

    private String address;
}
