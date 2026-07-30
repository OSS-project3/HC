package com.example.honorcitizen.domain.shipping.repository;

import com.example.honorcitizen.domain.shipping.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {

    Optional<ShippingAddress> findByApplicationId(Long applicationId);

    boolean existsByApplicationId(Long applicationId);
}
