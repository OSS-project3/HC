package com.example.honorcitizen.domain.user.dto;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.user.entity.User;
import lombok.Getter;

@Getter
public class LoginResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final UserRole role;
    private final String phone;
    private final String address;
    private final boolean restored;

    private LoginResponse(User user, boolean restored) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.phone = user.getPhone();
        this.address = user.getAddress();
        this.restored = restored;
    }

    public static LoginResponse of(User user, boolean restored) {
        return new LoginResponse(user, restored);
    }
}
