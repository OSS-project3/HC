package com.example.honorcitizen.domain.user.repository;

import com.example.honorcitizen.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOauthIdAndOauthProvider(String oauthId, String oauthProvider);

    boolean existsByOauthIdAndOauthProvider(String oauthId, String oauthProvider);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    // 계정 복구(아이디 찾기) 전용 — OAuth 전용 계정은 이메일 로그인 대상이 아니므로 매칭에서 제외한다.
    // 전화번호는 저장값을 정규화하지 않으므로 여기서 걸러내지 않는다 — 이름이 같은 후보 목록만 반환하고,
    // Service가 각 후보의 phone을 User.normalizePhone()로 정규화해 입력값과 비교한다(0/1/N건 판정은
    // Service 책임 — Optional<User>로는 "정확히 1건"과 "2건 이상"을 구분할 수 없어 List로 반환한다).
    @Query("SELECT u FROM User u WHERE TRIM(u.name) = :name AND u.passwordHash IS NOT NULL")
    List<User> findLocalAccountCandidatesByName(@Param("name") String name);
}
