package com.example.honorcitizen.domain.user.repository;

import com.example.honorcitizen.domain.user.entity.RefreshTokenSession;
import com.example.honorcitizen.domain.user.entity.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {

    Optional<RefreshTokenSession> findByTokenId(String tokenId);

    List<RefreshTokenSession> findByUserIdAndStatus(Long userId, RefreshTokenStatus status);

    void deleteByUserId(Long userId);
}
