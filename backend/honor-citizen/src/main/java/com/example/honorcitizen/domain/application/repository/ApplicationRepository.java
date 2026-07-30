package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    @Query("""
        SELECT a FROM Application a
        WHERE a.userId = :userId
        ORDER BY a.createdAt DESC
        """)
    List<Application> findAllByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT a FROM Application a
        WHERE a.id = :id
        """)
    Optional<Application> findByIdWithUser(@Param("id") Long id);

    boolean existsByUserIdAndStatusIn(Long userId, List<ApplicationStatus> statuses);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT a.status FROM Application a
        WHERE a.id = :id
        """)
    Optional<ApplicationStatus> findStatusById(@Param("id") Long id);

    List<Application> findAllByBulkOrderIdOrderByIdAsc(Long bulkOrderId);
}
