package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminAuthorizationService adminAuthorizationService;

    @Test
    void allowsAdmin() {
        User admin = mock(User.class);
        when(userService.findById(1L)).thenReturn(admin);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);

        assertThatCode(() -> adminAuthorizationService.requireAdmin(1L))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonAdmin() {
        User user = mock(User.class);
        when(userService.findById(2L)).thenReturn(user);
        when(user.getRole()).thenReturn(UserRole.USER);

        assertThatThrownBy(() -> adminAuthorizationService.requireAdmin(2L))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));
    }
}