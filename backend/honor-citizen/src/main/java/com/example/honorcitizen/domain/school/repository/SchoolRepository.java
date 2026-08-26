package com.example.honorcitizen.domain.school.repository;

import com.example.honorcitizen.domain.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolRepository extends JpaRepository<School, Long> {

    List<School> findByNameContainingIgnoreCaseOrderByNameAsc(String query);

    List<School> findAllByOrderByNameAsc();
}
