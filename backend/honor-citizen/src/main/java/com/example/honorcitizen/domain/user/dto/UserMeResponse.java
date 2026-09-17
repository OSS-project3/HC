package com.example.honorcitizen.domain.user.dto;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.user.entity.User;
import lombok.Getter;

// role은 새로고침 시 프론트가 관리자 UI를 서버 응답만으로 복원할 수 있도록 포함한다(2026-09-14 확정,
// 2026-08-20 결정을 정정). 실제 관리자 API 권한 판단은 여전히 서버 인가(SecurityConfig의 JWT role
// 클레임)로만 이뤄지며, 이 필드는 UI 표시 힌트일 뿐이다.
@Getter
public class UserMeResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String phone;
    private final String address;
    private final UserRole role;

    private UserMeResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.address = user.getAddress();
        this.role = user.getRole();
    }

    public static UserMeResponse from(User user) {
        return new UserMeResponse(user);
    }
}
