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
    // 된다. 같은 schoolId+orientation의 활성 디자인은 1개만 존재해야 한다 — 4-D부터는 관리자
    // 업로드 API(SchoolCardTemplateService)가 유일한 등록 경로이며, 정상 경로에서는 "있으면 UPDATE,
    // 없으면 CREATE"로만 동작해 2개가 생기지 않는다. 그 방어가 뚫린 경우(동시 요청 등)를 대비해
    // DB 레벨 unique index도 걸려 있다(`schema.sql`의 `card_designs_school_orientation_idx` —
    // H2가 partial index를 지원하지 않아 조건 없는 일반 unique index다, schema.sql 주석 참고).
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

    // 4-D: 관리자가 학교별 템플릿을 교체할 때 쓴다 — CardDesign.id는 그대로 유지하고 참조하는
    // UploadFile id만 바꾼다(이미 이 디자인으로 카드를 생성한 멤버가 있어도 그 결과물(구운 PNG)엔
    // 영향 없음, 앞으로 새로 생성하는 것부터 새 템플릿 적용).
    public void replaceTemplates(Long templateFrontId, Long templateBackId) {
        this.templateFrontId = templateFrontId;
        this.templateBackId = templateBackId;
    }
}
