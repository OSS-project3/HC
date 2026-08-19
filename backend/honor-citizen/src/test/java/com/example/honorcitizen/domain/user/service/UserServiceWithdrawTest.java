package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.ApplicationDailyLimit;
import com.example.honorcitizen.domain.application.repository.ApplicationDailyLimitRepository;
import com.example.honorcitizen.domain.user.entity.RefreshTokenSession;
import com.example.honorcitizen.domain.user.entity.RefreshTokenStatus;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.RefreshTokenSessionRepository;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// WITHDRAW-4: 회원탈퇴가 User/RefreshTokenSession/ApplicationDailyLimit을 하드 삭제하는지 검증한다.
@SpringBootTest
class UserServiceWithdrawTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;
    @Autowired
    private ApplicationDailyLimitRepository applicationDailyLimitRepository;

    private User user;

    @BeforeEach
    void setUp() {
        applicationDailyLimitRepository.deleteAll();
        refreshTokenSessionRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.saveAndFlush(
                User.createOAuthUser("withdraw-test@example.com", "oauth-withdraw", "google", "홍길동"));
    }

    @Test
    void withdrawHardDeletesUser() {
        userService.withdraw(user.getId(), "dummy-access-token");

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    void withdrawHardDeletesRefreshTokenSessions() {
        refreshTokenSessionRepository.saveAndFlush(RefreshTokenSession.active(
                user.getId(), "session-1", "token-1", "refresh-token-value", LocalDateTime.now().plusDays(14)));

        userService.withdraw(user.getId(), "dummy-access-token");

        assertThat(refreshTokenSessionRepository.findByUserIdAndStatus(user.getId(), RefreshTokenStatus.ACTIVE))
                .isEmpty();
        assertThat(refreshTokenSessionRepository.count()).isZero();
    }

    @Test
    void withdrawHardDeletesApplicationDailyLimit() {
        applicationDailyLimitRepository.saveAndFlush(
                ApplicationDailyLimit.createFirst(user.getId(), LocalDate.now()));

        userService.withdraw(user.getId(), "dummy-access-token");

        assertThat(applicationDailyLimitRepository.findByUserIdAndCountDate(user.getId(), LocalDate.now()))
                .isEmpty();
    }

    @Test
    void withdrawTwiceThrowsUserNotFoundOnSecondCall() {
        userService.withdraw(user.getId(), "dummy-access-token");

        assertThatThrownBy(() -> userService.withdraw(user.getId(), "dummy-access-token"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}
