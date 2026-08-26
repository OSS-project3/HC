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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// designNumber는 CardImageCompositor가 참조하는 classpath 리소스 디렉터리
// (card-templates/{cardType}/{designNumber}/)와 1:1로 대응한다(2-A). DB PK를 그 번호로 쓰지
// 않는 이유는 PK가 재사용 대상 시퀀스라 리소스 디렉터리명과 우연히 어긋날 수 있기 때문이다.
@Entity
@Table(name = "card_designs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"card_type_id", "design_number"}))
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

    @Column(nullable = false)
    private int designNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardDesignOrientation orientation;

    private Long templateFrontId;

    private Long templateBackId;

    @Column(nullable = false)
    private boolean isDefault;

    @Column(nullable = false)
    private boolean active;

    public static CardDesign create(Long cardTypeId, String name, int designNumber,
            CardDesignOrientation orientation, Long templateFrontId, Long templateBackId, boolean isDefault) {
        CardDesign design = new CardDesign();
        design.cardTypeId = cardTypeId;
        design.name = name;
        design.designNumber = designNumber;
        design.orientation = orientation;
        design.templateFrontId = templateFrontId;
        design.templateBackId = templateBackId;
        design.isDefault = isDefault;
        design.active = true;
        return design;
    }

    public void deactivate() {
        this.active = false;
    }
}
