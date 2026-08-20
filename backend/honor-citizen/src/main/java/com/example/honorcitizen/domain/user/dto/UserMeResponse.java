package com.example.honorcitizen.domain.user.dto;

import com.example.honorcitizen.domain.user.entity.User;
import lombok.Getter;

// 회원등급(role)은 내 정보 조회 응답에 포함하지 않는다(2026-08-20 확정) — 관리자 권한 판단은
// 서버 인가(SecurityConfig의 JWT role 클레임)로만 이뤄지고, 이 DTO는 순수 사용자 표시 정보다.
@Getter
public class UserMeResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String phone;
    private final String address;

    private UserMeResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.address = user.getAddress();
    }

    public static UserMeResponse from(User user) {
        return new UserMeResponse(user);
    }
}
