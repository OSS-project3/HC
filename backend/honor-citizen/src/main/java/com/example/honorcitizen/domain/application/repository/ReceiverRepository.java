package com.example.honorcitizen.domain.application.repository;

import com.example.honorcitizen.domain.application.entity.Receiver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiverRepository extends JpaRepository<Receiver, Long> {

    Optional<Receiver> findByApplicationId(Long applicationId);
}
