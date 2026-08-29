package com.example.honorcitizen.domain.card.repository;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardDesignRepository extends JpaRepository<CardDesign, Long> {

    List<CardDesign> findByCardTypeIdOrderByDesignNumber(Long cardTypeId);

    List<CardDesign> findByCardTypeIdAndActiveOrderByDesignNumber(Long cardTypeId, boolean active);

    // 4-B: 학생증 전용 조회 축 — cardTypeId가 아니라 schoolId+orientation으로 디자인을 찾는다.
    List<CardDesign> findBySchoolIdAndOrientationOrderByDesignNumber(Long schoolId, CardDesignOrientation orientation);

    List<CardDesign> findBySchoolIdAndOrientationAndActiveOrderByDesignNumber(
            Long schoolId, CardDesignOrientation orientation, boolean active);
}
