package com.example.honorcitizen.domain.user.repository;

import com.example.honorcitizen.common.enums.UserStatus;
import com.example.honorcitizen.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOauthIdAndOauthProvider(String oauthId, String oauthProvider);

    boolean existsByOauthIdAndOauthProvider(String oauthId, String oauthProvider);

    Optional<User> findByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    List<User> findByStatusAndAnonymizedAtIsNullAndWithdrawalRequestedAtBefore(
            UserStatus status, LocalDateTime threshold);
}
