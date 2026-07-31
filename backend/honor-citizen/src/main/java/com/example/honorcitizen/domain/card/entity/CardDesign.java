package com.example.honorcitizen.domain.card.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.CardDesignOrientation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_designs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardDesign extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cardTypeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardDesignOrientation orientation;

    private Long templateFrontId;

    private Long templateBackId;

    @Column(nullable = false)
    private boolean isDefault;

    @Column(nullable = false)
    private boolean active;

    public static CardDesign create(Long cardTypeId, String name, CardDesignOrientation orientation,
            Long templateFrontId, Long templateBackId, boolean isDefault) {
        CardDesign design = new CardDesign();
        design.cardTypeId = cardTypeId;
        design.name = name;
        design.orientation = orientation;
        design.templateFrontId = templateFrontId;
        design.templateBackId = templateBackId;
        design.isDefault = isDefault;
        design.active = true;
        return design;
    }
}
