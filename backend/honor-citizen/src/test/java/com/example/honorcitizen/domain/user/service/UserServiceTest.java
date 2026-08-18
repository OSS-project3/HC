package com.example.honorcitizen.domain.user.service;

import com.example.honorcitizen.common.enums.UserStatus;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void anonymizesUsersPastGracePeriod() {
        User expired = withdrawnUserBackdatedBy(8, "oauth-expired");

        int processed = userService.anonymizeExpiredWithdrawnUsers();

        entityManager.flush();
        entityManager.clear();
        User reloaded = userRepository.findById(expired.getId()).orElseThrow();
        assertThat(processed).isEqualTo(1);
        assertThat(reloaded.getAnonymizedAt()).isNotNull();
        assertThat(reloaded.getEmail()).isEqualTo("withdrawn-" + expired.getId() + "@anonymized.local");
        assertThat(reloaded.getName()).isEqualTo("탈퇴한 사용자");
        assertThat(reloaded.getOauthProvider()).isEqualTo("ANONYMIZED");
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @Test
    void doesNotTouchUsersStillWithinGracePeriod() {
        User recent = withdrawnUserBackdatedBy(1, "oauth-recent");

        int processed = userService.anonymizeExpiredWithdrawnUsers();

        entityManager.clear();
        User reloaded = userRepository.findById(recent.getId()).orElseThrow();
        assertThat(processed).isEqualTo(0);
        assertThat(reloaded.getAnonymizedAt()).isNull();
        assertThat(reloaded.getEmail()).isEqualTo("recent@example.com");
    }

    @Test
    void doesNotReprocessAlreadyAnonymizedUsers() {
        withdrawnUserBackdatedBy(10, "oauth-already");
        userService.anonymizeExpiredWithdrawnUsers();
        entityManager.flush();
        entityManager.clear();

        int processedAgain = userService.anonymizeExpiredWithdrawnUsers();

        assertThat(processedAgain).isEqualTo(0);
    }

    private User withdrawnUserBackdatedBy(int daysAgo, String oauthId) {
        String email = daysAgo == 1 ? "recent@example.com" : "expired-" + oauthId + "@example.com";
        User user = User.createOAuthUser(email, oauthId, "google", "탈퇴예정자");
        user.withdraw();
        user = userRepository.saveAndFlush(user);

        entityManager.createQuery("UPDATE User u SET u.withdrawalRequestedAt = :ts WHERE u.id = :id")
                .setParameter("ts", LocalDateTime.now().minusDays(daysAgo))
                .setParameter("id", user.getId())
                .executeUpdate();
        entityManager.clear();

        return userRepository.findById(user.getId()).orElseThrow();
    }
}
