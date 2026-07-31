package com.example.honorcitizen.domain.card.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.CardTypeCode;
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

import java.math.BigDecimal;

@Entity
@Table(name = "card_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardType extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private CardTypeCode code;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    public static CardType create(CardTypeCode code, String name, String description, BigDecimal price) {
        CardType cardType = new CardType();
        cardType.code = code;
        cardType.name = name;
        cardType.description = description;
        cardType.price = price;
        cardType.active = true;
        return cardType;
    }

    public boolean isStudentCard() {
        return this.code == CardTypeCode.STUDENT;
    }
}
