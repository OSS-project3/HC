package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.entity.Receiver;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Application·Applicant·Receiver·ApplicationMember 엔티티 생성을 담당하는 팩토리 컴포넌트.
 *
 * [이 클래스가 존재하는 이유]
 * ApplicationPersistenceService에서 엔티티 정적 팩토리 메서드(Application.createIndividual 등)를
 * 직접 호출하면 두 가지 문제가 생긴다:
 *
 * 1. 테스트 어려움: PersistenceService를 단위 테스트할 때 엔티티 생성 로직을 모킹하거나
 *    분리하기 어렵다. 팩토리를 별도 Bean으로 두면 Mockito로 팩토리만 교체해 테스트할 수 있다.
 *
 * 2. 변경 전파 범위: 엔티티 생성자 시그니처가 바뀌면(예: 새 필드 추가) 직접 호출한 곳을 모두
 *    찾아 수정해야 한다. 팩토리를 통하면 수정 지점이 팩토리 메서드 하나로 집중된다.
 *
 * [패키지 가시성]
 * package-private(default)으로 선언해 같은 패키지의 PersistenceService만 사용하도록 제한한다.
 * 외부 패키지(예: Controller, 다른 Service)에서 이 팩토리를 직접 사용하면 생성 책임이 분산되므로 차단한다.
 */
@Component
class ApplicationFactory {

    /**
     * 개인 신청용 Application 루트 엔티티를 생성한다.
     *
     * logoFileId, sealFileId는 학생증 카드에서만 값이 있고 나머지 카드 타입에서는 null이다.
     * 엔티티 생성 시점에 상태는 DRAFT로 초기화되며, 결제 완료 이후 PENDING으로 전이된다.
     */
    Application createIndividualApplication(Long userId, String applicationNumber, Long cardTypeId,
            IssueType issueType, boolean receiverSameAsApplicant, Long logoFileId, Long sealFileId,
            Orientation orientation, SchoolType schoolType, String schoolName) {
        return Application.createIndividual(userId, applicationNumber, cardTypeId, issueType,
                receiverSameAsApplicant, logoFileId, sealFileId, orientation, schoolType, schoolName);
    }

    /**
     * 개인 신청자(Applicant) 엔티티를 생성한다.
     *
     * Applicant는 Application과 1:1이며, 신청서를 제출한 사람의 연락처 정보를 담는다.
     * 단체 신청의 경우 조직명·부서 필드가 추가되므로 Applicant.createGroup()을 별도로 사용한다.
     * (단체 신청의 Applicant 생성은 PersistenceService에서 직접 호출해 처리한다.)
     */
    Applicant createIndividualApplicant(Long applicationId, String name, String email, String phone) {
        return Applicant.createIndividual(applicationId, name, email, phone);
    }

    /**
     * 수령인이 신청자와 동일한 경우, Applicant 정보를 복사해 Receiver를 생성한다.
     *
     * 사용 시점: receiverSameAsApplicant=true인 경우.
     * 복사본을 별도 엔티티로 저장하는 이유: Applicant 정보가 나중에 변경되어도
     * 배송 당시의 수령인 정보를 보존해야 운송장 출력·감사 이력 추적이 가능하다.
     *
     * 현재 PersistenceService에서 직접 호출하지 않고 있어 실질적으로 미사용 상태다.
     * saveReceiverIfNeeded에서는 항상 createIndividualReceiver를 사용하므로,
     * 추후 흐름 변경 시 이 메서드를 활용하면 된다.
     */
    Receiver copyIndividualReceiver(Long applicationId, Applicant applicant) {
        return Receiver.copyFromApplicant(applicationId, applicant);
    }

    /**
     * 수령인이 신청자와 다를 때, 직접 입력된 배송 정보로 Receiver를 생성한다.
     *
     * organizationName, department를 null로 넘기는 이유:
     * 개인 신청의 수령인은 개인이므로 단체 정보가 없다.
     * Receiver 엔티티가 개인·단체 양쪽을 하나의 테이블로 관리하므로
     * 개인 수령인의 경우 조직 관련 컬럼은 null로 저장된다.
     */
    Receiver createIndividualReceiver(Long applicationId, String name, String phone, String zipCode,
            String address, String detailAddress, String deliveryRequest) {
        return Receiver.create(applicationId, name, phone, zipCode, address, detailAddress, deliveryRequest,
                null, null);
    }

    /**
     * 개인 신청 멤버(ApplicationMember) 엔티티를 생성한다.
     *
     * ApplicationMember는 실제 카드를 받을 당사자의 신상 정보와 사진 경로를 담는다.
     * 개인 신청에서는 Application 1개 당 ApplicationMember 1개가 항상 생성된다.
     *
     * studentId, department는 학생증 카드가 아니면 null로 전달된다.
     * 카드 발급 완료 후 cardFrontPath, cardBackPath가 채워지며 초기에는 null이다.
     *
     * address는 카드에 인쇄되는 주소 — 학생증을 제외한 카드종류는 신청 입력값으로 받는다(null이면 검증 단계에서 거절).
     */
    ApplicationMember createIndividualMember(Long applicationId, String englishName, LocalDate birthDate,
            String nationality, LocalTime birthTime, String birthRegion, Gender gender, LocalDate entryDate,
            String studentId, String department, String photoPath, String address) {
        return ApplicationMember.createIndividual(applicationId, englishName, birthDate, nationality,
                birthTime, birthRegion, gender, entryDate, studentId, department, photoPath, address);
    }
}
