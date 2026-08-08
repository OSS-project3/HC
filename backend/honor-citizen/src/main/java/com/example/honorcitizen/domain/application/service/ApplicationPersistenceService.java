package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.entity.Receiver;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Application·Applicant·Receiver·ApplicationMember를 한 트랜잭션 안에 저장하는 서비스.
 *
 * [왜 ApplicationService에서 분리됐는가 — self-invocation 문제]
 * Spring의 @Transactional은 Spring AOP 프록시를 통해 동작한다.
 * ApplicationService가 자신의 메서드를 내부에서 this.saveIndividual()로 호출하면
 * AOP 프록시를 거치지 않고 실제 객체에 직접 접근하므로 @Transactional이 무시된다.
 *
 * 해결 방법은 여러 가지다:
 * - @Lazy 자기 참조 주입 (코드가 복잡해지고 순환 의존성 경고 발생)
 * - ApplicationContext에서 직접 꺼내기 (Spring 강결합)
 * - 별도 Bean으로 분리 → 이 클래스가 선택한 방법
 *
 * 별도 Bean으로 분리하면 ApplicationService → [프록시] → ApplicationPersistenceService
 * 경로로 호출되므로 @Transactional이 정상 적용된다.
 *
 * [이 클래스의 책임]
 * 오직 DB 저장만 담당한다. S3 업로드, 검증, 번호 채번은 ApplicationService에서 처리하고
 * S3 업로드가 끝난 UploadFile 메타데이터와 완성된 데이터를 이 클래스에 전달한다.
 * 단일 책임 원칙을 지켜 트랜잭션 경계와 저장 로직이 한 곳에 모이도록 설계했다.
 *
 * [패키지 가시성]
 * package-private으로 선언해 외부에서 직접 이 서비스를 호출하지 못하도록 제한한다.
 * ApplicationService를 통해서만 접근해야 검증→업로드→저장 순서가 보장된다.
 */
@Service
@RequiredArgsConstructor
class ApplicationPersistenceService {

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final ReceiverRepository receiverRepository;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final UploadFileRepository uploadFileRepository;
    // 엔티티 생성 로직을 위임 — 생성자 시그니처 변경 시 수정 지점 집중화
    private final ApplicationFactory applicationFactory;

    /**
     * 개인 신청 저장.
     *
     * 저장 순서:
     * 1. Application (루트 엔티티, ID 생성됨)
     * 2. Applicant (Application.id 외래키 필요)
     * 3. Receiver (MOBILE_AND_PHYSICAL 타입에서만, Applicant.id 외래키 필요)
     * 4. ApplicationMember (Application.id 외래키 필요, photoPath는 S3 키)
     *
     * 이 순서를 지켜야 외래키 제약 조건을 만족하면서 저장할 수 있다.
     * @Transactional이 적용되므로 4개 엔티티가 모두 저장되거나 모두 롤백된다.
     *
     * @param photoPath S3에 업로드 완료된 사진 경로 — 이 메서드는 S3와 통신하지 않는다.
     */
    @Transactional
    Application saveIndividual(Long userId, String applicationNumber, Long cardTypeId, IssueType issueType,
            boolean receiverSameAsApplicant, UploadedFileMetadata logoFileMetadata, UploadedFileMetadata sealFileMetadata,
            ApplicationCreateRequest request, String applicantEmail, String photoPath) {

        Long logoFileId = saveUploadFileMetadataIfPresent(logoFileMetadata);
        Long sealFileId = saveUploadFileMetadataIfPresent(sealFileMetadata);

        // 1. Application 루트 엔티티 저장 — 이 시점에 auto-increment ID가 생성된다.
        Application application = applicationFactory.createIndividualApplication(
                userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant, logoFileId, sealFileId);
        applicationRepository.save(application);

        // 2. Applicant 저장 — application.getId()를 외래키로 사용하므로 1번 이후에 저장한다.
        Applicant applicant = applicationFactory.createIndividualApplicant(
                application.getId(), request.getApplicant().getName(), applicantEmail,
                request.getApplicant().getPhone());
        applicantRepository.save(applicant);

        // 3. Receiver 조건부 저장
        // MOBILE 전용 발급이면 실물 배송이 없으므로 수령인 정보가 불필요하다.
        // MOBILE_AND_PHYSICAL일 때만 수령인을 저장해 불필요한 데이터를 DB에 남기지 않는다.
        saveReceiverIfNeeded(application, request, applicant);

        // 4. ApplicationMember 저장 — 개인 신청은 항상 멤버 1명
        ApplicationCreateRequest.MemberRequest memberRequest = request.getMember();
        ApplicationMember member = applicationFactory.createIndividualMember(
                application.getId(), memberRequest.getEnglishName(), memberRequest.getBirthDate(),
                memberRequest.getNationality(), memberRequest.getBirthTime(), memberRequest.getBirthRegion(),
                memberRequest.getGender(), memberRequest.getEntryDate(),
                memberRequest.getStudentId(), memberRequest.getDepartment(), photoPath);
        applicationMemberRepository.save(member);

        return application;
    }

    /**
     * 단체 신청 저장.
     *
     * 저장 순서:
     * 1. Application (루트 엔티티)
     * 2. Applicant (단체 신청자 — 조직명·부서 포함)
     * 3. Receiver (MOBILE_AND_PHYSICAL 타입에서만)
     * 4. ApplicationMember N개 (ZIP에서 파싱된 멤버 수만큼)
     *
     * [멤버 루프 저장 방식]
     * 멤버를 for 루프로 한 명씩 save하는 방식을 사용한다.
     * saveAll(List)을 쓰지 않은 이유: 엔티티 수가 많아도 배치 INSERT가 자동으로 되지 않으며,
     * 현재 hibernate.jdbc.batch_size 설정이 없으므로 실질적인 차이가 없다.
     * 추후 성능 이슈 발생 시 @BatchSize 또는 JdbcTemplate 배치 INSERT로 전환을 검토한다.
     *
     * @param totalQuantity ZIP 파싱 결과 멤버 수 — Application 엔티티에 총 수량으로 저장된다.
     * @param memberUploads S3 업로드 완료된 사진 경로가 포함된 멤버 목록
     */
    @Transactional
    Application saveGroup(Long userId, String applicationNumber, Long cardTypeId, IssueType issueType,
            boolean receiverSameAsApplicant, int totalQuantity, UploadedFileMetadata logoFileMetadata,
            UploadedFileMetadata sealFileMetadata, UploadedFileMetadata submitFileMetadata,
            BulkApplicationCreateRequest request, String applicantEmail, Iterable<GroupMemberUpload> memberUploads) {

        Long logoFileId = saveUploadFileMetadataIfPresent(logoFileMetadata);
        Long sealFileId = saveUploadFileMetadataIfPresent(sealFileMetadata);
        Long submitFileId = saveUploadFileMetadataIfPresent(submitFileMetadata);

        // 1. Application 루트 엔티티 저장 — 단체는 totalQuantity, submitFileId가 추가된다.
        Application application = Application.createGroup(
                userId, applicationNumber, cardTypeId, issueType, receiverSameAsApplicant, totalQuantity,
                logoFileId, sealFileId, submitFileId);
        applicationRepository.save(application);

        // 2. Applicant 저장 — 단체 신청자는 개인과 달리 조직명·부서를 추가로 저장한다.
        //    이 정보는 카드 발급 후 배송 운송장과 세금계산서 발행에 활용된다.
        BulkApplicationCreateRequest.ApplicantRequest applicantRequest = request.getApplicant();
        Applicant applicant = Applicant.createGroup(application.getId(), applicantRequest.getName(),
                applicantEmail, applicantRequest.getPhone(),
                applicantRequest.getOrganizationName(), applicantRequest.getDepartment());
        applicantRepository.save(applicant);

        // 3. Receiver 조건부 저장 — 단체 수령인은 개인과 달리 조직명·부서도 함께 보관한다.
        saveGroupReceiverIfNeeded(application, request, applicant);

        // 4. 멤버 N명 저장
        // 이 시점에 멤버별 사진은 이미 S3에 업로드 완료된 상태다.
        // GroupMemberUpload가 BulkMemberRow(파싱 데이터)와 photoPath(S3 키)를 묶어 가져온다.
        for (GroupMemberUpload upload : memberUploads) {
            BulkMemberRow row = upload.row();
            ApplicationMember member = ApplicationMember.createGroupRow(
                    application.getId(), row.englishName(), row.birthDate(), row.nationality(),
                    row.birthTime(), row.birthRegion(), row.gender(), row.entryDate(),
                    row.email(), row.phone(), row.address(), row.studentId(), row.department(), upload.photoPath());
            applicationMemberRepository.save(member);
        }

        return application;
    }

    private Long saveUploadFileMetadataIfPresent(UploadedFileMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        UploadFile uploadFile = UploadFile.create(
                metadata.originalName(), metadata.storedName(), metadata.filePath(),
                metadata.fileType(), metadata.mimeType(), metadata.fileSize());
        return uploadFileRepository.save(uploadFile).getId();
    }

    /**
     * MOBILE_AND_PHYSICAL 발급 타입에서만 개인 수령인을 저장한다.
     *
     * [이름·전화번호 대체 로직]
     * 수령인 이름·전화번호가 비어있으면 신청자(Applicant) 정보로 자동 대체한다.
     * 이유: 프론트엔드에서 "수령인이 신청자와 동일" 체크박스를 선택한 경우
     *       수령인 필드가 비어있는 상태로 전송되기 때문이다.
     *       빈 값을 그대로 저장하면 배송지 정보가 없어 운송장 출력이 불가능해진다.
     *
     * 우편번호·주소·상세주소·배송 요청은 대체하지 않는다. 신청자 정보에 주소가 없기 때문이다.
     */
    private void saveReceiverIfNeeded(Application application, ApplicationCreateRequest request, Applicant applicant) {
        if (request.getIssueType() != IssueType.MOBILE_AND_PHYSICAL) {
            // MOBILE 전용은 수령인 불필요 — 저장하지 않고 바로 리턴
            return;
        }
        ApplicationCreateRequest.ReceiverRequest receiverRequest = request.getReceiver();
        // 이름·전화가 비면 신청자 정보로 대체한다.
        String name = StringUtils.hasText(receiverRequest.getName()) ? receiverRequest.getName() : applicant.getName();
        String phone = StringUtils.hasText(receiverRequest.getPhone()) ? receiverRequest.getPhone() : applicant.getPhone();
        Receiver receiver = applicationFactory.createIndividualReceiver(application.getId(), name, phone,
                receiverRequest.getZipCode(), receiverRequest.getAddress(), receiverRequest.getDetailAddress(),
                receiverRequest.getDeliveryRequest());
        receiverRepository.save(receiver);
    }

    /**
     * MOBILE_AND_PHYSICAL 발급 타입에서만 단체 수령인을 저장한다.
     *
     * 개인 수령인과의 차이점:
     * - organizationName, department를 추가로 저장한다.
     *   단체 신청의 경우 배송지가 회사·학교일 수 있으며 운송장에 부서명이 필요한 경우가 있다.
     *
     * [이름·전화번호 대체 로직]
     * 단체 신청의 수령인 이름·전화가 비면 단체 신청자(Applicant) 정보로 대체한다.
     * 단체 신청자는 담당자이므로 담당자 연락처가 배송 수령인으로 적합하다.
     */
    private void saveGroupReceiverIfNeeded(Application application, BulkApplicationCreateRequest request, Applicant applicant) {
        if (request.getIssueType() != IssueType.MOBILE_AND_PHYSICAL) {
            return;
        }
        BulkApplicationCreateRequest.ReceiverRequest receiverRequest = request.getReceiver();
        String name = StringUtils.hasText(receiverRequest.getName()) ? receiverRequest.getName() : applicant.getName();
        String phone = StringUtils.hasText(receiverRequest.getPhone()) ? receiverRequest.getPhone() : applicant.getPhone();
        Receiver receiver = Receiver.create(application.getId(), name, phone,
                receiverRequest.getZipCode(), receiverRequest.getAddress(), receiverRequest.getDetailAddress(),
                receiverRequest.getDeliveryRequest(), receiverRequest.getOrganizationName(), receiverRequest.getDepartment());
        receiverRepository.save(receiver);
    }
}
