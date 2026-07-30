package com.example.honorcitizen.domain.bulk.repository;

import com.example.honorcitizen.domain.bulk.entity.BulkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkOrderRepository extends JpaRepository<BulkOrder, Long> {
}
