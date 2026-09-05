package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 유스케이스의 공통 Service 계층 인가 진입점.
 *
 * <p>{@code /api/admin/**}의 Security 검증을 1차 경계로 유지하면서, Service를 다른 코드에서 직접
 * 호출하더라도 관리자 권한이 보장되도록 2차 검증한다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final UserService userService;

    @Transactional(readOnly = true)
    public void requireAdmin(Long userId) {
        User user = userService.findById(userId);
        if (user.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
