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

    // 학생증(STUDENT)일 때만 값이 있다(4-B) — 그 외 카드종류는 항상 null. 학생증 디자인은
    // 카드종류가 아니라 학교마다 다르므로, 조회 축이 cardTypeId 하나가 아니라 schoolId+orientation이
    // 된다. 같은 schoolId+orientation의 활성 디자인은 1개만 존재해야 한다(운영자가 직접 DB에 넣는
    // 로우라 이 불변조건은 애플리케이션 레벨 INSERT 게이트가 없음 — 운영 절차로 지켜야 한다).
    private Long schoolId;

    // 기존 호출부(학생증 아닌 카드종류) 하위 호환용 — schoolId 없이 호출하면 null로 생성한다.
    public static CardDesign create(Long cardTypeId, String name, int designNumber,
            CardDesignOrientation orientation, Long templateFrontId, Long templateBackId, boolean isDefault) {
        return create(cardTypeId, name, designNumber, orientation, templateFrontId, templateBackId, isDefault, null);
    }

    public static CardDesign create(Long cardTypeId, String name, int designNumber,
            CardDesignOrientation orientation, Long templateFrontId, Long templateBackId, boolean isDefault,
            Long schoolId) {
        CardDesign design = new CardDesign();
        design.cardTypeId = cardTypeId;
        design.name = name;
        design.designNumber = designNumber;
        design.orientation = orientation;
        design.templateFrontId = templateFrontId;
        design.templateBackId = templateBackId;
        design.isDefault = isDefault;
        design.active = true;
        design.schoolId = schoolId;
        return design;
    }

    public void deactivate() {
        this.active = false;
    }
}
