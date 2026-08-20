package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.UploadFileType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.enums.LookupMethod;
import com.example.honorcitizen.domain.application.dto.ApplicationCardDownloadResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationCancelResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationPhotoReuploadResponse;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationDetailResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationListItemResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.entity.Receiver;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.storage.StorageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
//S3 작업에는 DB 트랜잭션을 걸고 싶지 않고 DB 저장 부분에만 @Transactional을 걸고 싶은데, 같은 ApplicationService
// 내부 메서드로 분리하면 self-invocation 때문에 프록시를 안 거치므로 DB 저장 로직을 별도 Bean으로 분리한 것.
/**
 * 명예 카드 신청의 생성·조회·사진 재업로드·카드 다운로드 유스케이스를 조율하는 진입점 서비스.
 *
 * [설계 핵심 원칙]
 * 1. 스토리지(S3) 업로드와 DB 저장은 원자적으로 묶을 수 없으므로 의도적으로 순서를 분리한다.
 *    - S3 업로드를 먼저 수행하고, 업로드된 키를 uploadedKeys 리스트에 누적한다.
 *    - DB 저장이 실패하면 catch 블록에서 uploadedKeys를 역순으로 삭제해 스토리지 누수를 방지한다.
 *    - 역순 삭제인 이유: 나중에 올린 파일이 앞에 올린 파일에 의존할 수 있어 의존 역순으로 제거한다.
 *
 *
 * 2. @Transactional이 필요한 DB 저장 로직은 ApplicationPersistenceService에 위임한다.
 *    - 같은 Bean 내부에서 this.method()로 호출하면 Spring AOP 프록시를 우회해
 *      @Transactional 어노테이션이 전혀 적용되지 않는 self-invocation 문제가 발생한다.
 *    - 별도 Bean으로 분리하면 스프링이 프록시 객체를 통해 호출하므로 트랜잭션이 정상 적용된다.
 *
 * 3. 입력 검증은 항상 스토리지 업로드 이전에 수행한다.
 *    - 잘못된 입력으로 인한 S3 업로드 비용 발생을 방지한다.
 *    - 검증 실패 시 아직 업로드된 파일이 없으므로 롤백 비용도 없다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    // presigned URL 유효 기간: 7일
    // 카드 발급 완료 알림을 받은 후 사용자가 여유 있게 다운로드할 수 있도록 넉넉히 설정.
    // 기간이 너무 짧으면 알림 수신 후 즉시 접속하지 않으면 만료되는 UX 문제가 생긴다.
    private static final long CARD_DOWNLOAD_URL_EXPIRY_SECONDS = 7 * 24 * 60 * 60L;

    // 마이페이지 신청 목록(api.md API 6) 페이지 크기 상한 — Board/Event와 동일한 관례.
    private static final int MAX_PAGE_SIZE = 100;

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final ReceiverRepository receiverRepository;
    private final CardTypeRepository cardTypeRepository;
    private final UploadFileRepository uploadFileRepository;
    private final AdminActivityLogRepository adminActivityLogRepository;
    private final UserService userService;
    // 트랜잭션 경계를 위한 별도 Bean — 위 클래스 주석의 self-invocation 문제 참고
    private final ApplicationPersistenceService applicationPersistenceService;
    // 일일 신청 생성 횟수 제한(하루 3회, 개인/단체 합산) 전담 — APPLICATION.md §7
    private final ApplicationDailyLimitService applicationDailyLimitService;
    // 사진 파일 크기·MIME·바이너리 시그니처·해상도 검증 전담 컴포넌트
    private final ApplicationPhotoValidator applicationPhotoValidator;
    private final StorageService storageService;
    // ZIP 파일에서 엑셀을 파싱하고 사진을 매칭하는 단체 신청 전용 파서
    private final BulkExcelParser bulkExcelParser;

    // JPA Repository가 아닌 네이티브 쿼리(SELECT nextval)로 DB 시퀀스를 직접 채번해야 하므로
    // EntityManager를 직접 주입받는다. @Autowired 대신 @PersistenceContext를 사용하는 이유는
    // JPA 스펙에 맞는 컨텍스트 범위 관리(EntityManager는 스레드 안전하지 않음)를 위해서다.
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 개인 신청 생성.
     *
     * 처리 순서:
     * 1. 신청 자격 검증 (약관 동의 여부, 탈퇴 여부 등)
     * 2. 활성화된 카드 타입 조회 (폐지된 카드 타입으로 신청 차단)
     * 3. 입력값 검증 (수령인 유무, 사진, 학생 전용 필드)
     * 4. 신청번호 채번 (DB 시퀀스, 동시성 안전)
     * 5. S3 파일 업로드 (로고·직인·사진)
     * 6. DB 저장 — 실패 시 5번에서 올린 파일 역순 삭제
     */
    public ApplicationCreateResponse createIndividual(Long userId, ApplicationCreateRequest request,
            MultipartFile photo, MultipartFile schoolLogo, MultipartFile schoolSeal) {

        User user = findUser(userId);
        //학생증이라면, 로고가 필요하고 직인 선택, 학번 학과 검증이 되어야 함.
        CardType cardType = findActiveCardType(request.getCardTypeId());
        boolean isStudent = cardType.isStudentCard();

        // 입력 검증을 S3 업로드 이전에 먼저 수행한다.
        // 사진 검증(ApplicationPhotoValidator)은 파일을 통째로 메모리에 읽어야 하므로
        // 리소스 소비가 있지만, S3 업로드보다는 훨씬 저렴하다. -> S3업로드 되면, 다시 삭제해야하는 불편함 발생.
        validateCreateIndividual(request, photo, schoolLogo, schoolSeal, isStudent);

        // 하루 3회 제한(개인/단체 합산) — 파일 업로드 이전에 자리를 먼저 확정해 실패 시 슬롯 낭비 없이
        // 빠르게 거절한다. 실패(아래 catch)하면 예약한 자리를 반환한다.
        LocalDate today = ApplicationDailyLimitService.today();
        reserveDailyLimitSlot(userId, today);

        String applicationNumber = generateApplicationNumber();
        // uploadedKeys: S3에 업로드된 객체 키 목록. DB 저장 실패 시 이 목록을 역순 순회해 삭제한다.
        List<String> uploadedKeys = new ArrayList<>();

        try {
            // 학생증 카드에만 학교 로고가 필수이고 직인은 선택이다.
            // 비학생 카드에 로고·직인이 들어오면 validateStudentFields에서 이미 차단했으므로 여기서는 조건만 확인한다.
            UploadedFileMetadata logoFileMetadata = isStudent ? uploadFileToStorage(schoolLogo, UploadFileType.PHOTO, uploadedKeys) : null;
            UploadedFileMetadata sealFileMetadata = isStudent && isPresent(schoolSeal)
                    ? uploadFileToStorage(schoolSeal, UploadFileType.PHOTO, uploadedKeys)
                    : null;
            boolean receiverSameAsApplicant = request.isReceiverSameAsApplicant();
            String photoPath = storePhotoFile(applicationNumber, photo, uploadedKeys);

            // 신청자가 이메일을 직접 입력하지 않은 경우 OAuth2 로그인 계정의 이메일을 사용한다.
            // 외국인 사용자 중 별도 연락용 이메일을 지정하고 싶은 경우를 위해 직접 입력도 허용한다.
            String applicantEmail = hasText(request.getApplicant().getEmail()) ? request.getApplicant().getEmail() : user.getEmail();

            Application application = applicationPersistenceService.saveIndividual(
                    userId, applicationNumber, cardType.getId(), request.getIssueType(), receiverSameAsApplicant,
                    logoFileMetadata, sealFileMetadata, request, applicantEmail, photoPath);
            return ApplicationCreateResponse.from(application);
        } catch (RuntimeException e) {
            // DB 저장이 실패하면 이미 S3에 업로드한 파일들을 역순으로 제거해 고아 파일이 남지 않도록 한다.
            // 역순인 이유: 파일 간 의존 관계가 있을 경우(예: 메타데이터가 원본을 참조) 의존 역순으로 지워야 안전하다.
            deleteUploadedFilesQuietlyReversed(uploadedKeys);
            applicationDailyLimitService.releaseSlot(userId, today);
            throw e;
        }
    }

    // ApplicationDailyLimitService.reserveSlot()이 던지는 DataIntegrityViolationException은
    // "오늘 첫 신청 row를 두 요청이 동시에 만들려다 유니크 제약이 충돌한" 경우다 — 새 트랜잭션으로
    // 한 번만 재시도하면 먼저 커밋된 row를 보고 정상적으로 락을 잡고 증가시킨다(재시도도 실패하면
    // 그대로 예외를 전파해 INTERNAL_ERROR로 처리한다 — 같은 충돌이 두 번 연속 나는 건 비정상 상황).
    private void reserveDailyLimitSlot(Long userId, LocalDate today) {
        try {
            applicationDailyLimitService.reserveSlot(userId, today);
        } catch (DataIntegrityViolationException e) {
            applicationDailyLimitService.reserveSlot(userId, today);
        }
    }

    // 개인 신청 입력값 검증을 순서대로 위임한다.
    // 수령인 유무 → 사진 → 학생 전용 필드 순으로 검증하는 이유:
    //   수령인은 비즈니스 규칙(IssueType에 따라 필수/불가), 사진은 파일 I/O, 학생 필드는 카드 타입 의존이므로
    //   논리 검증 → 파일 검증 → 조건부 검증 순으로 빠른 실패(fail-fast)를 유도한다.
    private void validateCreateIndividual(ApplicationCreateRequest request, MultipartFile photo,
            MultipartFile schoolLogo, MultipartFile schoolSeal, boolean isStudent) {
        validateReceiverPresence(request);
        applicationPhotoValidator.validateFacePhoto(photo);
        validateStudentFields(isStudent, request.getOrientation(), request.getSchoolType(), request.getSchoolName(),
                request.getMember().getStudentId(), request.getMember().getDepartment(), schoolLogo, schoolSeal);
    }

    // 신청 자격 검증을 UserService에 위임한다.
    // 단순 findById가 아닌 findEligibleApplicationUser를 호출하는 이유:
    //   약관 미동의 사용자, 탈퇴 처리 중인 사용자 등은 신청이 불가하며 이 검증을 UserService가 담당한다.
    private User findUser(Long userId) {
        return userService.findEligibleApplicationUser(userId);
    }

    /**
     * 단체 신청 생성.
     *
     * 처리 순서:
     * 1. 신청 자격 검증 (약관 동의 여부, 탈퇴 여부 등) — 개인 신청과 동일하게 모든 파일 처리보다 먼저 수행한다.
     * 2. 활성화된 카드 타입 조회
     * 3. 수령인 유무 검증 및 로고·직인 존재 검증
     * 4. ZIP 파싱 (BulkExcelParser) — 오류 있으면 즉시 전체 실패 반환
     * 5. 신청번호 채번 및 파일 업로드 (로고, 직인, ZIP 원본, 멤버별 사진)
     * 6. DB 저장 — 실패 시 5번에서 올린 파일 역순 삭제
     *
     * [ZIP 파싱과 파일 업로드 순서]
     * ZIP 파싱(4번)이 파일 업로드(5번)보다 먼저인 이유:
     *   ZIP 내용이 유효하지 않으면 파일 업로드 자체를 막아 불필요한 S3 비용과 롤백 처리를 없앤다.
     *   파싱은 메모리 내 작업이라 S3 호출보다 훨씬 빠르다.
     *
     * [멤버 사진 업로드]
     * 멤버별 사진은 ZIP에서 byte[]로 읽힌 뒤 각각 S3에 업로드된다.
     * 업로드 완료된 경로와 행 데이터를 GroupMemberUpload에 묶어 DB 저장 단계로 전달한다.
     */
    public BulkApplicationCreateResponse createGroup(Long userId, BulkApplicationCreateRequest request,
            MultipartFile logo, MultipartFile seal, MultipartFile submitFile) {
        User user = findUser(userId);
        CardType cardType = findActiveCardType(request.getCardTypeId());
        boolean isStudent = cardType.isStudentCard();

        // 수령인 검증 먼저 수행 (IssueType에 따라 필수/불가).
        // 로고·직인 검증: 비학생 카드는 직인도 필수, 학생 카드는 직인 선택.
        // 로고가 없으면 카드에 학교를 인증할 수단이 없으므로 항상 필수다.
        validateGroupReceiverPresence(request);
        if (!isPresent(logo) || (!isStudent && !isPresent(seal))) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        // orientation·schoolType·schoolName은 개인 신청과 동일하게 신청서 전체에 1개, 학생증일 때만 필수
        // (orientation/schoolType은 2026-08-14, schoolName은 2026-08-19 신규 — 단체 신청은 항상 한 학교 단위로 접수된다는 전제).
        // 학번·학과는 단체는 여전히 첨부 엑셀(BulkExcelParser)로만 받으므로 여기서는 검증하지 않는다.
        boolean hasStudentApplicationField = request.getOrientation() != null || request.getSchoolType() != null
                || hasText(request.getSchoolName());
        if (isStudent && (request.getOrientation() == null || request.getSchoolType() == null
                || !isValidSchoolName(request.getSchoolName()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (!isStudent && hasStudentApplicationField) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // ZIP 파싱 — 엑셀의 모든 행을 검증하고 오류가 있으면 BulkValidationException으로 전체 실패.
        // 개별 행의 오류를 하나씩 반환하는 게 아니라 모든 오류를 한 번에 errors[] 배열로 반환하는 이유:
        //   사용자가 한 번에 전체 오류를 파악하고 ZIP을 다시 만들 수 있게 UX를 개선하기 위해서다.
        List<BulkMemberRow> rows = bulkExcelParser.parse(submitFile, isStudent, request.getSchoolType());

        // 하루 3회 제한(개인/단체 합산) — 단체 신청도 개인 신청과 동일하게 파일 업로드 이전에 자리를 확정한다.
        LocalDate today = ApplicationDailyLimitService.today();
        reserveDailyLimitSlot(userId, today);

        String applicationNumber = generateApplicationNumber();
        List<String> uploadedKeys = new ArrayList<>();

        try {
            UploadedFileMetadata logoFileMetadata = uploadFileToStorage(logo, UploadFileType.PHOTO, uploadedKeys);
            UploadedFileMetadata sealFileMetadata = isPresent(seal)
                    ? uploadFileToStorage(seal, UploadFileType.PHOTO, uploadedKeys)
                    : null;

            // 원본 ZIP 파일도 UploadFile 엔티티로 DB에 등록한다.
            // 이유: 관리자가 검토 중 원본 ZIP 파일을 다시 다운로드해 내용을 확인할 수 있어야 하고,
            //       사진 재업로드 시 구 ZIP ID를 추적해 S3에서 삭제해야 하므로 DB에 ID를 보관한다.
            UploadedFileMetadata submitFileMetadata = uploadFileToStorage(submitFile, UploadFileType.ZIP, uploadedKeys);
            boolean receiverSameAsApplicant = request.getReceiver() == null || request.getReceiver().isSameAsApplicant();

            // 각 멤버 사진을 S3에 업로드하고, 업로드된 경로와 행 데이터를 함께 GroupMemberUpload에 담는다.
            // 이 시점에는 아직 DB에 아무것도 저장되지 않았으므로 업로드 실패 시 uploadedKeys 롤백으로 충분하다.
            List<GroupMemberUpload> memberUploads = new ArrayList<>();
            for (BulkMemberRow row : rows) {
                String photoPath = storePhotoBytes(applicationNumber, row.photoFilename(), row.photoBytes(), uploadedKeys);
                memberUploads.add(new GroupMemberUpload(row, photoPath));
            }

            String applicantEmail = hasText(request.getApplicant().getEmail()) ? request.getApplicant().getEmail() : user.getEmail();

            Application application = applicationPersistenceService.saveGroup(
                    userId, applicationNumber, cardType.getId(), request.getIssueType(), receiverSameAsApplicant,
                    rows.size(), logoFileMetadata, sealFileMetadata, submitFileMetadata, request, applicantEmail, memberUploads);
            return BulkApplicationCreateResponse.from(application);
        } catch (RuntimeException e) {
            // DB 저장 실패 시 업로드된 모든 파일(로고, 직인, ZIP, 멤버 사진 전부)을 역순 삭제한다.
            deleteUploadedFilesQuietlyReversed(uploadedKeys);
            applicationDailyLimitService.releaseSlot(userId, today);
            throw e;
        }
    }

    /**
     * 마이페이지 신청 목록(api.md API 6) — 로그인 사용자 본인 신청만 페이지네이션 조회.
     *
     * 정렬은 항상 createdAt DESC로 고정한다(id DESC를 tie-breaker로 추가해 동일 시각 생성분의
     * 순서를 안정적으로 보장). status는 선택 필터이며 null이면 전체 상태를 조회한다.
     */
    @Transactional(readOnly = true)
    public PageResponse<MyApplicationListItemResponse> listMyApplications(Long userId, ApplicationStatus status,
            int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Application> applications = status == null
                ? applicationRepository.findByUserId(userId, pageable)
                : applicationRepository.findByUserIdAndStatus(userId, status, pageable);

        // 카드종류명은 Application에 직접 없어 배치 조회 후 매핑한다(Review 패턴과 동일).
        Map<Long, CardType> cardTypeById = cardTypeRepository
                .findAllById(applications.getContent().stream().map(Application::getCardTypeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CardType::getId, Function.identity()));

        return PageResponse.from(applications, application ->
                MyApplicationListItemResponse.of(application, cardTypeById.get(application.getCardTypeId()).getName()));
    }

    /**
     * 마이페이지 신청 상세(api.md API 7) — 본인 소유 신청만 조회 가능.
     *
     * receiver는 issueType=MOBILE_AND_PHYSICAL일 때만 존재하며, 그 외에는 null을 응답한다.
     * 단체 신청은 구성원 개별 목록 대신 memberCount만 노출한다(별도 API로 분리 예정).
     */
    @Transactional(readOnly = true)
    public MyApplicationDetailResponse getMyApplicationDetail(Long userId, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        CardType cardType = cardTypeRepository.findById(application.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Applicant applicant = applicantRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Receiver receiver = application.getIssueType() == IssueType.MOBILE_AND_PHYSICAL
                ? receiverRepository.findByApplicationId(applicationId).orElse(null)
                : null;
        long memberCount = applicationMemberRepository.countByApplicationId(applicationId);

        return MyApplicationDetailResponse.of(application, cardType.getName(), applicant, receiver, memberCount);
    }

    /**
     * 관리자 신청 목록 — 소유자 무관 전체 조회. listMyApplications와 동일한 정렬·응답 형태이되
     * userId 필터만 없다(응답 DTO는 마이페이지와 동일해 그대로 재사용한다 — 관리자만 볼 수 있는
     * 별도 필드가 아직 없어 신규 DTO를 만들 이유가 없음).
     */
    @Transactional(readOnly = true)
    public PageResponse<MyApplicationListItemResponse> listApplicationsForAdmin(Long adminId, ApplicationStatus status,
            int page, int size) {
        validateAdmin(adminId);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Application> applications = status == null
                ? applicationRepository.findAll(pageable)
                : applicationRepository.findByStatus(status, pageable);

        Map<Long, CardType> cardTypeById = cardTypeRepository
                .findAllById(applications.getContent().stream().map(Application::getCardTypeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(CardType::getId, Function.identity()));

        return PageResponse.from(applications, application ->
                MyApplicationListItemResponse.of(application, cardTypeById.get(application.getCardTypeId()).getName()));
    }

    /** 관리자 신청 상세 — 소유권 체크 없이 어떤 신청이든 조회 가능. 나머지는 getMyApplicationDetail과 동일. */
    @Transactional(readOnly = true)
    public MyApplicationDetailResponse getApplicationDetailForAdmin(Long adminId, Long applicationId) {
        validateAdmin(adminId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        CardType cardType = cardTypeRepository.findById(application.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Applicant applicant = applicantRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Receiver receiver = application.getIssueType() == IssueType.MOBILE_AND_PHYSICAL
                ? receiverRepository.findByApplicationId(applicationId).orElse(null)
                : null;
        long memberCount = applicationMemberRepository.countByApplicationId(applicationId);

        return MyApplicationDetailResponse.of(application, cardType.getName(), applicant, receiver, memberCount);
    }

    /**
     * 로그인 사용자가 본인 신청을 취소한다.
     *
     * Application 상태 변경과 일일 신청 슬롯 반환은 하나의 트랜잭션에서 처리한다.
     * 따라서 낙관적 락 충돌이나 commit 실패가 발생하면 CANCELLED 전이와 카운터 감소가 함께 롤백된다.
     * 이미 취소된 신청은 Entity가 false를 반환하며, 이 경우 슬롯을 다시 반환하지 않는 멱등 성공이다.
     *
     * S3 파일 정리는 DB commit 이후에만 실행해야 하므로 이번 DB 전이 단위에는 포함하지 않는다.
     * 후속 단위에서 최초 취소 성공 여부를 기준으로 after-commit 정리를 연결한다.
     */
    @Transactional
    public ApplicationCancelResponse cancelByUser(Long userId, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        // 멱등 여부보다 소유권을 먼저 확인해 타인에게 신청의 존재·취소 상태가 노출되지 않도록 한다.
        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        boolean firstCancellation = application.cancelByUser(LocalDateTime.now());
        if (firstCancellation) {
            completeCancellation(application);
        }

        return ApplicationCancelResponse.from(application);
    }

    @Transactional
    public Application guidePayment(Long adminId, Long applicationId) {
        validateAdmin(adminId);
        Application application = findApplication(applicationId);
        application.guidePayment(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        return application;
    }

    @Transactional
    public Application confirmPayment(Long adminId, Long applicationId) {
        validateAdmin(adminId);
        Application application = findApplication(applicationId);
        boolean firstConfirmation = application.confirmPayment();
        if (firstConfirmation) {
            adminActivityLogRepository.save(AdminActivityLog.create(
                    adminId, AdminActivityLog.PAYMENT_CONFIRMED, applicationId, "Payment confirmed"));
        }
        return application;
    }

    @Transactional
    public boolean cancelForPaymentTimeout(Long applicationId, LocalDateTime cancelledAt) {
        Application application = findApplication(applicationId);
        boolean firstCancellation = application.cancelForPaymentTimeout(cancelledAt);
        if (firstCancellation) {
            completeCancellation(application);
        }
        return firstCancellation;
    }

    private Application findApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    // 2026-08-19 정책 변경: 탈퇴 계정은 즉시 하드 삭제되므로 findById가 성공했다는 것 자체가
    // "탈퇴하지 않은 계정"이라는 뜻이다 — 별도 상태 체크가 필요 없다.
    private void validateAdmin(Long adminId) {
        User admin = userService.findById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void completeCancellation(Application application) {
        List<String> fileKeysToDelete = clearCancellationFileReferences(application);
        applicationDailyLimitService.releaseSlot(
                application.getUserId(), ApplicationDailyLimitService.toCountDate(application.getCreatedAt()));
        registerCancellationS3CleanupAfterCommit(fileKeysToDelete);
    }

    private List<String> clearCancellationFileReferences(Application application) {
        List<String> fileKeys = new ArrayList<>();
        List<Long> uploadFileIds = new ArrayList<>();
        if (application.getLogoFileId() != null) {
            uploadFileIds.add(application.getLogoFileId());
        }
        if (application.getSealFileId() != null) {
            uploadFileIds.add(application.getSealFileId());
        }
        if (application.getSubmitFileId() != null) {
            uploadFileIds.add(application.getSubmitFileId());
        }

        List<UploadFile> managedFiles = uploadFileRepository.findAllById(uploadFileIds);
        managedFiles.stream()
                .map(UploadFile::getFilePath)
                .filter(this::hasText)
                .forEach(fileKeys::add);

        List<ApplicationMember> members = applicationMemberRepository.findByApplicationId(application.getId());
        members.stream()
                .map(ApplicationMember::getPhotoPath)
                .filter(this::hasText)
                .forEach(fileKeys::add);

        application.clearManagedFileReferences();
        members.forEach(ApplicationMember::clearPhoto);
        uploadFileRepository.deleteAll(managedFiles);
        return fileKeys;
    }

    private void registerCancellationS3CleanupAfterCommit(List<String> fileKeys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Cancellation S3 cleanup requires an active transaction");
        }

        List<String> fileKeysSnapshot = List.copyOf(fileKeys);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteUploadedFilesQuietlyReversed(fileKeysSnapshot);
            }
        });
    }

    /**
     * 사진 재업로드 (PHOTO_REJECTED 상태에서만 허용).
     *
     * [개인 신청 재업로드 흐름]
     *   1. 새 사진을 S3에 업로드
     *   2. ApplicationMember의 photoPath를 새 경로로 교체
     *   3. Application 상태를 PHOTO_REJECTED → REVIEWING(재검토 요청)으로 전이
     *   4. DB commit 이후 구 사진을 S3에서 삭제
     *   새 파일 저장 성공을 확인한 후에 구 파일을 지우는 순서가 중요하다.
     *   거꾸로 하면 저장 실패 시 사진 자체가 없어질 수 있다.
     *
     * [단체 신청 재업로드 흐름]
     *   단체 신청은 멤버가 N명이므로 개별 수정 대신 전체 멤버를 삭제하고 재적재한다.
     *   이유: 어떤 멤버의 어떤 사진이 반려됐는지 클라이언트가 특정할 필요 없이
     *         ZIP 전체를 교체하는 단순한 UX를 제공하기 위해서다.
     *   1. 구 멤버 사진 경로 목록을 미리 수집 (deleteByApplicationId 이후에는 조회 불가)
     *   2. 새 ZIP 파싱 및 멤버별 사진 S3 업로드
     *   3. 기존 ApplicationMember 전체 삭제 후 새 멤버로 재적재
     *   4. Application.resubmitForReview()로 상태 전이 및 submitFileId 교체
     *   5. DB commit 이후 구 사진들 S3에서 삭제
     */
    @Transactional
    public ApplicationPhotoReuploadResponse reuploadPhoto(Long userId, Long applicationId,
            MultipartFile photo, MultipartFile submitFile) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        // 다른 사용자의 신청에 접근하는 것을 차단한다.
        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 사진 재업로드는 관리자가 사진을 반려(PHOTO_REJECTED)한 경우에만 허용된다.
        // 다른 상태(예: PENDING, REVIEWING)에서 사진을 교체하면 이미 진행 중인 검토가 무효화될 수 있어 차단한다.
        if (application.getStatus() != ApplicationStatus.PHOTO_REJECTED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        List<String> newUploadedKeys = new ArrayList<>();

        try {
            if (application.isIndividual()) {
                // 개인 신청: 단일 사진 파일만 교체한다. ZIP을 함께 보내면 의도가 명확하지 않으므로 오류 처리한다.
                if (!isPresent(photo) || isPresent(submitFile)) {
                    throw new CustomException(ErrorCode.INVALID_INPUT);
                }
                ApplicationMember member = applicationMemberRepository.findByApplicationId(applicationId).get(0);
                String oldPhotoPath = member.getPhotoPath();

                // 새 사진을 먼저 업로드하고 경로를 교체한다.
                // 이 순서를 지키면 업로드 실패 시 구 사진이 그대로 남아 데이터 무결성을 보장한다.
                member.updatePhoto(storePhotoFile(application.getApplicationNumber(), photo, newUploadedKeys));
                application.resubmitForReview(null); // submitFileId는 개인 신청에서 null
                registerS3CleanupAfterTransaction(newUploadedKeys, java.util.Collections.singletonList(oldPhotoPath));
            } else {
                // 단체 신청: ZIP 전체 교체. 단일 사진 파일을 보내면 오류 처리한다.
                if (!isPresent(submitFile) || isPresent(photo)) {
                    throw new CustomException(ErrorCode.INVALID_INPUT);
                }
                CardType cardType = cardTypeRepository.findById(application.getCardTypeId())
                        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
                List<BulkMemberRow> rows = bulkExcelParser.parse(submitFile, cardType.isStudentCard(), application.getSchoolType());

                Long oldSubmitFileId = application.getSubmitFileId();
                String oldSubmitFilePath = oldSubmitFileId == null ? null : uploadFileRepository.findById(oldSubmitFileId)
                        .map(UploadFile::getFilePath)
                        .orElse(null);

                // 구 멤버 사진 경로를 deleteByApplicationId 호출 전에 반드시 수집해야 한다.
                // 삭제 후에는 해당 레코드가 사라지므로 경로를 조회할 수 없다.
                List<String> oldMemberPhotoPaths = new ArrayList<>(applicationMemberRepository.findByApplicationId(applicationId).stream()
                        .map(ApplicationMember::getPhotoPath)
                        .toList());
                if (hasText(oldSubmitFilePath)) {
                    oldMemberPhotoPaths.add(oldSubmitFilePath);
                }

                UploadedFileMetadata newSubmitFileMetadata = uploadFileToStorage(submitFile, UploadFileType.ZIP, newUploadedKeys);
                Long newSubmitFileId = saveUploadFileMetadata(newSubmitFileMetadata);

                // 기존 멤버를 전체 삭제 후 새 멤버로 재적재한다.
                // 멤버 수가 다를 수도 있고(ZIP에서 행이 추가/삭제될 수 있음), 어떤 멤버가 반려됐는지
                // 매핑을 유지하는 것보다 전체 교체가 구현 복잡도 측면에서 훨씬 단순하다.
                applicationMemberRepository.deleteByApplicationId(applicationId);
                for (BulkMemberRow row : rows) {
                    String photoPath = storePhotoBytes(application.getApplicationNumber(), row.photoFilename(), row.photoBytes(), newUploadedKeys);
                    ApplicationMember member = ApplicationMember.createGroupRow(
                            applicationId, row.englishName(), row.birthDate(), row.nationality(),
                            row.birthTime(), row.birthRegion(), row.gender(), row.entryDate(),
                            row.email(), row.phone(), row.address(), row.studentId(), row.department(), photoPath);
                    applicationMemberRepository.save(member);
                }
                application.updateTotalQuantity(rows.size());
                application.resubmitForReview(newSubmitFileId);
                registerS3CleanupAfterTransaction(newUploadedKeys, oldMemberPhotoPaths);
            }
        } catch (RuntimeException e) {
            deleteUploadedFilesQuietlyReversed(newUploadedKeys);
            throw e;
        }

        return ApplicationPhotoReuploadResponse.from(application);
    }

    /**
     * 카드 이미지 다운로드 URL 생성.
     *
     * [개인 신청]
     * - 앞면·뒷면 각각의 presigned URL을 직접 반환한다.
     *
     * [단체 신청]
     * - 멤버가 N명이므로 모든 카드 이미지를 메모리에서 ZIP으로 묶어 S3에 임시 업로드한 뒤
     *   해당 ZIP의 presigned URL을 반환한다.
     * - 임시 ZIP 파일은 만료 후 수동 정리가 필요하다 (현재 미구현 — 추후 S3 Lifecycle 규칙 추가 권장).
     *
     * presigned URL 만료 시각(expiresAt)을 응답에 포함하는 이유:
     *   클라이언트가 URL이 만료되기 전에 다운로드를 시작할 수 있도록 UI 안내 메시지에 활용한다.
     */
    @Transactional(readOnly = true)
    public ApplicationCardDownloadResponse getCardDownload(Long userId, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        // 본인 소유 검증 — 다른 사용자의 카드 이미지 URL을 획득하는 것을 차단한다.
        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 카드 다운로드는 발급 완료(COMPLETED) 상태에서만 허용한다.
        // COMPLETED 미만의 상태(예: PENDING, REVIEWING)에서는 카드 이미지 자체가 아직 생성되지 않았으므로
        // cardFrontPath, cardBackPath가 null이어서 presigned URL 생성 자체가 불가능하다.
        if (application.getStatus() != ApplicationStatus.COMPLETED) {
            throw new CustomException(ErrorCode.CARD_NOT_READY);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(CARD_DOWNLOAD_URL_EXPIRY_SECONDS);
        List<ApplicationMember> members = applicationMemberRepository.findByApplicationId(applicationId);

        if (application.isIndividual()) {
            ApplicationMember member = members.get(0);
            String cardFrontUrl = storageService.generatePresignedUrl(member.getCardFrontPath(), CARD_DOWNLOAD_URL_EXPIRY_SECONDS);
            String cardBackUrl = storageService.generatePresignedUrl(member.getCardBackPath(), CARD_DOWNLOAD_URL_EXPIRY_SECONDS);
            return ApplicationCardDownloadResponse.forIndividual(applicationId, cardFrontUrl, cardBackUrl, expiresAt);
        }

        // 단체: 멤버 카드 전체를 ZIP으로 묶어 presigned URL로 제공
        String downloadUrl = buildGroupCardsZipAndGetUrl(application, members);
        return ApplicationCardDownloadResponse.forGroup(applicationId, downloadUrl, expiresAt);
    }

    /**
     * 단체 멤버 카드 이미지 전체를 인메모리 ZIP으로 묶어 S3에 업로드하고 presigned URL을 반환한다.
     *
     * [파일명 레이블 규칙]
     * - 멤버의 영문명을 파일명으로 사용한다 (예: JohnDoe-front.png, JohnDoe-back.png).
     * - 영문명이 없으면 순번(1, 2, 3...)을 사용해 파일명 충돌을 방지한다.
     *
     * [임시 ZIP S3 경로]
     * - applications/{applicationNumber}/cards/{UUID}.zip 으로 저장된다.
     * - UUID를 포함하는 이유: 같은 신청에서 여러 번 다운로드를 요청해도 경로가 겹치지 않도록 하기 위해서다.
     * - 이 ZIP은 presigned URL 만료 후 사용되지 않으므로 S3 Lifecycle 규칙으로 자동 삭제하는 것을 권장한다.
     *
     * [메모리 사용]
     * - 모든 카드 이미지를 ByteArrayOutputStream에 담으므로 멤버 수가 많으면 메모리 압박이 생긴다.
     * - 대규모 단체(수백 명 이상)는 스트리밍 방식으로 개선이 필요하다.
     */
    private String buildGroupCardsZipAndGetUrl(Application application, List<ApplicationMember> members) {
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
            int index = 1;
            for (ApplicationMember member : members) {
                // 영문명이 없는 경우 순번을 레이블로 사용해 파일명이 null이 되지 않도록 한다.
                String label = member.getEnglishName() != null ? member.getEnglishName() : String.valueOf(index);
                zip.putNextEntry(new ZipEntry(label + "-front.png"));
                zip.write(storageService.download(member.getCardFrontPath()));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry(label + "-back.png"));
                zip.write(storageService.download(member.getCardBackPath()));
                zip.closeEntry();
                index++;
            }
        } catch (java.io.IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }

        String key = "applications/" + application.getApplicationNumber() + "/cards/" + UUID.randomUUID() + ".zip";
        storageService.uploadBytes(key, zipBytes.toByteArray(), "application/zip");
        return storageService.generatePresignedUrl(key, CARD_DOWNLOAD_URL_EXPIRY_SECONDS);
    }

    /**
     * 신청 조회 (비로그인 공개 API).
     *
     * 두 가지 조회 방식을 지원한다:
     * - CARD 방식: 발급된 카드번호로 조회. 카드를 받은 후 카드 번호로 본인 신청을 확인할 때 사용.
     * - APPLICATION 방식: 신청번호 + 전화번호 + 이메일로 조회.
     *   신청번호만으로는 누구든 신청 상태를 볼 수 있어 개인정보 노출 위험이 있으므로
     *   전화번호와 이메일 모두 일치해야만 조회를 허용한다.
     *
     * [이름 마스킹]
     * 조회 결과에서 신청자 이름을 가운데 마스킹해 반환한다.
     * 이유: 카드번호나 신청번호를 우연히 알게 된 제3자가 타인의 이름을 완전히 알 수 없도록 한다.
     *
     * [오류 응답 정책]
     * APPLICATION 방식에서 신청번호는 존재하지만 전화·이메일이 불일치하는 경우,
     * FORBIDDEN이 아닌 NOT_FOUND를 반환한다.
     * 이유: 신청번호의 존재 자체를 알리면 공격자가 신청번호를 열거(enumeration)해
     *       어떤 번호가 유효한지 확인할 수 있기 때문이다.
     */
    @Transactional(readOnly = true)
    public ApplicationLookupResponse lookup(ApplicationLookupRequest request) {
        // APPLICATION 방식은 전화·이메일 모두 입력해야 한다.
        // 하나라도 없으면 본인 확인을 우회할 수 있으므로 즉시 차단한다.
        if (request.getMethod() == LookupMethod.APPLICATION
                && ((request.getPhone() == null || request.getPhone().isBlank())
                        || (request.getEmail() == null || request.getEmail().isBlank()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Application application = request.getMethod() == LookupMethod.CARD
                ? lookupByCard(request)
                : lookupByApplicationNumber(request);

        Applicant applicant = applicantRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        CardType cardType = cardTypeRepository.findById(application.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        return new ApplicationLookupResponse(
                application.getId(),
                application.getApplicationType(),
                application.getApplicationNumber(),
                maskName(applicant.getName()), // 개인정보 보호를 위해 성명 가운데 마스킹
                cardType.getName(),
                application.getStatus(),
                application.getPhotoRejectReason(),
                application.getCreatedAt());
    }

    // 카드번호로 ApplicationMember를 찾고, 해당 멤버가 속한 상위 Application을 반환한다.
    // 카드번호는 ApplicationMember에 저장되므로 Application을 바로 찾을 수 없어 2단계 조회가 필요하다.
    private Application lookupByCard(ApplicationLookupRequest request) {
        ApplicationMember member = applicationMemberRepository.findByCardNumber(request.getKeyValue())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return applicationRepository.findById(member.getApplicationId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    // 신청번호로 Application을 찾고, 전화번호·이메일이 모두 일치하는지 확인한다.
    // 불일치 시 FORBIDDEN이 아닌 NOT_FOUND를 반환해 신청번호 존재 여부가 노출되지 않도록 한다.
    private Application lookupByApplicationNumber(ApplicationLookupRequest request) {
        Application application = applicationRepository.findByApplicationNumber(request.getKeyValue())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Applicant applicant = applicantRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        if (!matches(request, applicant.getPhone(), applicant.getEmail())) {
            // 존재 자체를 알리지 않기 위해 NOT_FOUND 반환 (정보 열거 공격 방지)
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return application;
    }

    // 전화번호: 완전 일치 (공백·대소문자 그대로 비교)
    // 이메일: 대소문자 무시 비교 (example@GMAIL.COM과 example@gmail.com을 같다고 처리)
    // 두 항목 모두 일치해야 통과 — 하나만 맞춰도 조회 가능하면 추측 공격에 취약하다.
    private boolean matches(ApplicationLookupRequest request, String targetPhone, String targetEmail) {
        boolean phoneMatches = request.getPhone() != null && request.getPhone().equals(targetPhone);
        boolean emailMatches = request.getEmail() != null && request.getEmail().equalsIgnoreCase(targetEmail);
        return phoneMatches && emailMatches;
    }
//DB: 기존 단체 구성원을 전부 삭제하고 새 ZIP의 구성원으로 통째로 교체한다.
//S3: 기존 구성원의 사진도 지워야 하므로 DB에서 Member를 삭제하기 전에 옛날 사진 경로를 미리 기억해둔다.
    /**
     * 성명 마스킹 규칙:
     * - 1글자 이하: 그대로 반환 (마스킹할 글자가 없음)
     * - 2글자: 첫 글자 + * (예: 김* )
     * - 3글자 이상: 첫 글자 + *...* + 마지막 글자 (예: 김*원, John*oe)
     *
     * 첫 글자와 마지막 글자를 남기는 이유:
     *   본인 확인용으로 '내 이름이 맞는지' 대략 알 수 있게 하되,
     *   타인이 완전한 이름을 알 수 없도록 가운데를 가린다.
     */
    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    /**
     * 단체 신청 수령인 존재 여부를 IssueType에 따라 검증한다.
     *
     * - MOBILE_AND_PHYSICAL (모바일+실물): 실물 카드를 배송해야 하므로 수령인(배송지) 정보가 필수다.
     * - MOBILE (모바일 전용): 배송이 없으므로 수령인 정보가 불필요하다.
     *   수령인 정보가 함께 오면 의도가 모호해지므로 명시적으로 차단한다.
     */
    private void validateGroupReceiverPresence(BulkApplicationCreateRequest request) {
        if (request.getIssueType() == IssueType.MOBILE_AND_PHYSICAL && request.getReceiver() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.getIssueType() == IssueType.MOBILE && request.getReceiver() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    // 롤백 시 uploadedKeys를 넘기지 않는 오버로드 — 재업로드 처리처럼 롤백 추적이 별도 관리되는 경우에 사용
    private String storePhotoBytes(String applicationNumber, String originalFilename, byte[] bytes) {
        return storePhotoBytes(applicationNumber, originalFilename, bytes, new ArrayList<>());
    }

    /**
     * ZIP에서 읽은 byte[] 형태의 사진을 S3에 업로드하고 업로드된 키를 반환한다.
     *
     * [S3 키 구조]
     * applications/{applicationNumber}/member-photos/{UUID}-{sanitizedFilename}
     * - applicationNumber를 경로에 포함해 신청별로 파일을 그룹화한다.
     * - UUID를 앞에 붙여 같은 이름의 파일이 올라와도 덮어쓰지 않도록 고유성을 보장한다.
     * - sanitizedFilename은 원본 파일명에서 특수문자를 제거해 S3 키 규칙을 준수한다.
     *
     * uploadedKeys에 키를 추가하는 이유: DB 저장 실패 시 이 키 목록으로 S3 객체를 롤백 삭제한다.
     */
    private String storePhotoBytes(String applicationNumber, String originalFilename, byte[] bytes, List<String> uploadedKeys) {
        String key = "applications/" + applicationNumber + "/member-photos/" + UUID.randomUUID() + "-"
                + sanitizeFilename(originalFilename);
        storageService.uploadBytes(key, bytes, guessContentType(originalFilename));
        uploadedKeys.add(key);
        return key;
    }

    /**
     * 파일 확장자를 기반으로 Content-Type을 추정한다.
     *
     * 정확한 바이너리 시그니처 검증은 ApplicationPhotoValidator에서 이미 수행됐으므로
     * 여기서는 S3 업로드 시 필요한 Content-Type 헤더 값만 빠르게 결정한다.
     * webp를 별도 처리하는 이유: BulkExcelParser가 ZIP 내 webp 사진도 허용하기 때문이다
     * (개인 신청 사진은 ApplicationPhotoValidator에서 webp 확장자를 차단하므로 실제로는 단체에서만 쓰임).
     */
    private String guessContentType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg"; // jpg, jpeg 또는 알 수 없는 확장자는 jpeg로 처리
    }

    /**
     * 활성화된 카드 타입을 조회한다.
     *
     * filter(CardType::isActive)를 통해 비활성 카드 타입으로 신청하는 것을 차단한다.
     * 이유: 운영 중 특정 카드 타입을 일시 중단하거나 폐지해야 할 때 카드 타입을 삭제하면
     *       기존 신청 데이터의 참조 무결성이 깨질 수 있으므로, isActive=false로 소프트 비활성화한다.
     *       새 신청은 비활성 카드로 받지 않지만, 기존 신청 데이터는 그대로 유지된다.
     */
    private CardType findActiveCardType(Long cardTypeId) {
        return cardTypeRepository.findById(cardTypeId)
                .filter(CardType::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    /**
     * 개인 신청 수령인 존재 여부를 IssueType에 따라 검증한다.
     *
     * - MOBILE_AND_PHYSICAL: 실물 카드 배송을 위한 수령인 정보가 필수.
     * - MOBILE: 배송이 없으므로 수령인 정보를 전달하면 안 된다.
     *   이를 허용하면 클라이언트가 잘못된 데이터를 보내도 무시하게 되어
     *   의도하지 않은 동작의 원인을 찾기 어려워진다.
     */
    private void validateReceiverPresence(ApplicationCreateRequest request) {
        if (request.getIssueType() == IssueType.MOBILE_AND_PHYSICAL && request.getReceiver() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.getIssueType() == IssueType.MOBILE && request.getReceiver() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 학생증 카드에만 요구되는 필드(가로/세로형·학교구분·학번·학과·로고·직인)를 검증한다.
     *
     * ✅ 2026-08-14 확정: 학번·학과는 더 이상 "학생증이면 무조건 필수"가 아니라
     * "학생증 + 대학교 선택 시에만" 필수다. 고등학교를 선택하면 오히려 학번·학과가 있으면 안 된다
     * (대학교 특유의 식별정보라 고등학교엔 의미가 없기 때문). orientation·schoolType·로고는
     * 학교구분과 무관하게 학생증이면 항상 필수다.
     *
     * 세 방향으로 검증한다:
     * 1. 학생증인데 orientation/schoolType/로고가 없는 경우 → INVALID_INPUT
     * 2. 학생증+대학교인데 학번·학과가 없거나 형식이 틀린 경우 → INVALID_INPUT
     *    학생증+고등학교인데 학번·학과가 있는 경우 → INVALID_INPUT
     * 3. 비학생 카드인데 학생 전용 필드(orientation/schoolType/학번/학과/로고/직인) 중 하나라도 있는 경우 → INVALID_INPUT
     *    이유: 비학생 카드 신청에 이 값들이 포함되면 데이터 오염이 발생하고
     *          추후 카드 생성 로직에서 예기치 않은 동작을 유발할 수 있다.
     *
     * 학번 형식 검증(숫자만, 최대 10자)은 단순 정규식으로 처리하며 대학교마다 체계가 달라
     * 더 세밀한 검증은 하지 않는다.
     *
     * 로고·직인 이미지는 ApplicationPhotoValidator.validateSchoolAsset으로 검증한다.
     * validateFacePhoto와 달리 해상도 하한 검증이 없는 이유: 로고·직인은 카드에 비율 축소해 넣으므로
     * 300x400 기준이 의미가 없기 때문이다.
     */
    private void validateStudentFields(boolean isStudent, Orientation orientation, SchoolType schoolType,
            String schoolName, String studentId, String department, MultipartFile schoolLogo, MultipartFile schoolSeal) {
        boolean anyStudentFieldPresent = orientation != null || schoolType != null || hasText(schoolName)
                || hasText(studentId) || hasText(department) || isPresent(schoolLogo) || isPresent(schoolSeal);

        if (!isStudent) {
            if (anyStudentFieldPresent) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            return;
        }

        if (orientation == null || schoolType == null || !isPresent(schoolLogo) || !isValidSchoolName(schoolName)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        boolean studentIdOrDepartmentPresent = hasText(studentId) || hasText(department);
        if (schoolType == SchoolType.UNIVERSITY) {
            if (!hasText(studentId) || !hasText(department) || !isValidStudentId(studentId)) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
        } else if (studentIdOrDepartmentPresent) {
            // 고등학교는 학번·학과 자체가 없는 개념이라 값이 있으면 오류로 취급한다.
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 로고·직인은 파일 크기·MIME·바이너리 시그니처만 확인하고 해상도 하한 검증은 제외한다.
        applicationPhotoValidator.validateSchoolAsset(schoolLogo);
        if (isPresent(schoolSeal)) {
            applicationPhotoValidator.validateSchoolAsset(schoolSeal);
        }
    }

    // 학번은 숫자만 허용하고 최대 10자리로 제한한다.
    // 이유: 국내 대학 학번은 대부분 숫자 8~10자리이며, 다른 형식은 데이터 오염 위험이 있다.
    private boolean isValidStudentId(String studentId) {
        return studentId.matches("\\d{1,10}");
    }

    // 학교명은 (DTO getter에서 이미 트림된 값 기준으로) 5~20자, 한글·영문·숫자·공백만 허용한다.
    private boolean isValidSchoolName(String schoolName) {
        return schoolName != null && schoolName.length() >= 5 && schoolName.length() <= 20
                && schoolName.matches("[가-힣a-zA-Z0-9 ]+");
    }

    // S3 키가 비어있지 않을 때만 삭제를 시도한다.
    // 키가 null이거나 빈 문자열인 경우 S3 API 호출 자체를 막아 불필요한 네트워크 비용을 방지한다.
    private void deleteIfPresent(String key) {
        if (hasText(key)) {
            deleteUploadedFileQuietly(key);
        }
    }

    /**
     * 업로드된 파일들을 역순으로 삭제한다 (DB 저장 실패 시 롤백용).
     *
     * 역순 삭제인 이유:
     * 파일 업로드 순서가 logoFile → sealFile → photoFile 순이라면,
     * photoFile이 logoFile의 경로를 참조하는 메타데이터를 가질 수 있다.
     * 이런 의존 관계가 있을 때 의존하는 파일(photoFile)을 먼저 삭제해야
     * 참조가 끊긴 고아 파일이 남지 않는다.
     * 현재 파일들은 독립적이지만 향후 의존 관계 추가를 대비해 역순 삭제를 유지한다.
     */
    private void deleteUploadedFilesQuietlyReversed(List<String> uploadedKeys) {
        for (int i = uploadedKeys.size() - 1; i >= 0; i--) {
            deleteUploadedFileQuietly(uploadedKeys.get(i));
        }
    }

    private void deleteUploadedFileQuietly(String key) {
        try {
            storageService.delete(key);
        } catch (RuntimeException e) {
            log.warn("Failed to delete uploaded S3 object. key={}", key, e);
        }
    }

    private void registerS3CleanupAfterTransaction(List<String> newUploadedKeys, List<String> oldKeysToDeleteAfterCommit) {
        List<String> newKeysSnapshot = new ArrayList<>(newUploadedKeys);
        List<String> oldKeysSnapshot = oldKeysToDeleteAfterCommit.stream()
                .filter(this::hasText)
                .toList();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            oldKeysSnapshot.forEach(this::deleteIfPresent);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                oldKeysSnapshot.forEach(ApplicationService.this::deleteIfPresent);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteUploadedFilesQuietlyReversed(newKeysSnapshot);
                }
            }
        });
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isPresent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    // 롤백 추적이 필요 없는 경우(재업로드 등)의 오버로드 — 빈 리스트를 전달해 키 누적 없이 업로드만 수행
    private String storePhotoFile(String applicationNumber, MultipartFile photo) {
        return storePhotoFile(applicationNumber, photo, new ArrayList<>());
    }

    /**
     * MultipartFile 형태의 사진을 S3에 업로드하고 업로드된 키를 반환한다.
     *
     * storePhotoBytes와 동일한 S3 키 구조를 사용한다:
     * applications/{applicationNumber}/member-photos/{UUID}-{sanitizedFilename}
     *
     * MultipartFile과 byte[] 두 가지 오버로드가 있는 이유:
     * - 개인 신청은 HTTP 요청에서 직접 MultipartFile로 받으므로 storePhotoFile을 사용한다.
     * - 단체 신청은 ZIP에서 읽은 byte[]이므로 storePhotoBytes를 사용한다.
     * 두 경로를 통합하면 한쪽에서 불필요하게 byte[]로 변환하거나 임시 파일을 만들어야 한다.
     */
    private String storePhotoFile(String applicationNumber, MultipartFile photo, List<String> uploadedKeys) {
        String key = "applications/" + applicationNumber + "/member-photos/" + UUID.randomUUID() + "-"
                + sanitizeFilename(photo.getOriginalFilename());
        storageService.upload(key, photo);
        uploadedKeys.add(key);
        return key;
    }

    // 재업로드처럼 호출 메서드가 이미 @Transactional인 경로에서 사용하는 편의 메서드.
    private Long storeUploadFile(MultipartFile file, UploadFileType fileType) {
        UploadedFileMetadata metadata = uploadFileToStorage(file, fileType, new ArrayList<>());
        return saveUploadFileMetadata(metadata);
    }

    /**
     * 파일을 S3에만 업로드하고 DB 저장에 필요한 메타데이터를 반환한다.
     *
     * storePhotoFile·storePhotoBytes와의 차이점:
     * - 사진 파일(member-photos/)은 경로(S3 키)만 ApplicationMember에 저장한다.
     * - 이 메서드가 처리하는 파일(로고, 직인, ZIP)은 UploadFile 엔티티로 별도 관리한다.
     *   이유: 관리자가 이 파일들을 다시 다운로드할 수 있어야 하고, 사진 재업로드 시
     *         구 ZIP의 ID(submitFileId)를 추적해 S3에서 정리해야 하기 때문이다.
     * - 단, 신청 생성 경로에서는 UploadFile DB row를 여기서 저장하지 않는다.
     *   UploadFile row는 ApplicationPersistenceService의 @Transactional 안에서 Application과 함께 저장한다.
     *
     * [S3 키 구조]
     * applications/uploads/{UUID}-{sanitizedFilename}
     * - member-photos와 달리 applications/uploads/ 경로를 사용해 관리자 업로드 파일임을 구분한다.
     */
    private UploadedFileMetadata uploadFileToStorage(MultipartFile file, UploadFileType fileType, List<String> uploadedKeys) {
        String storedName = UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        String key = "applications/uploads/" + storedName;
        storageService.upload(key, file);
        uploadedKeys.add(key);

        return new UploadedFileMetadata(
                file.getOriginalFilename(), storedName, key, fileType, file.getContentType(), file.getSize());
    }

    private Long saveUploadFileMetadata(UploadedFileMetadata metadata) {
        UploadFile uploadFile = UploadFile.create(
                metadata.originalName(), metadata.storedName(), metadata.filePath(),
                metadata.fileType(), metadata.mimeType(), metadata.fileSize());
        return uploadFileRepository.save(uploadFile).getId();
    }

    /**
     * 파일명에서 S3 키로 사용할 수 없는 문자를 제거한다.
     *
     * S3 객체 키는 대부분의 문자를 허용하지만, URL 인코딩 문제·SDK 오동작·
     * 파일시스템 호환성을 위해 영문·숫자·점·하이픈·밑줄 외 모든 문자를 _ 로 치환한다.
     * 한글 파일명, 공백, 괄호 등이 대표적인 치환 대상이다.
     *
     * 앞에 UUID를 붙이므로 치환 후 모든 파일명이 file로 바뀌어도 고유성은 보장된다.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * 신청번호를 생성한다.
     * 형식: APP-{연도}-{6자리 시퀀스} (예: APP-2026-000001)
     *
     * 연도를 포함하는 이유: 연도별로 시퀀스를 구분해 사용자가 언제 신청했는지 번호만으로 파악 가능하다.
     * 6자리 시퀀스인 이유: 연간 최대 999,999건을 감당할 수 있어 충분하다.
     * 시퀀스가 연도별로 리셋되지 않는다는 점에 유의한다 (DB 시퀀스는 글로벌 단조 증가).
     */
    private String generateApplicationNumber() {
        int year = LocalDate.now().getYear();
        String prefix = "APP-" + year + "-";
        long sequence = nextApplicationSequence();
        return prefix + String.format("%06d", sequence);
    }

    /**
     * PostgreSQL 시퀀스(application_seq)에서 다음 값을 채번한다.
     *
     * 시퀀스를 사용하는 이유:
     * - MAX(id) + 1이나 UUID 방식과 달리 트랜잭션 격리 수준과 무관하게 중복 없는 번호를 보장한다.
     * - 여러 요청이 동시에 들어와도 각 요청이 고유한 번호를 받는다.
     * - 트랜잭션 롤백이 발생해도 시퀀스는 롤백되지 않으므로 번호에 공백이 생길 수 있지만,
     *   신청번호는 비연속이어도 무방하다.
     *
     * Number 캐스팅: PostgreSQL은 nextval 결과를 BigDecimal로 반환할 수 있으므로
     * Number 상위 타입으로 받아 longValue()를 호출해 타입 불일치를 방지한다.
     */
    private long nextApplicationSequence() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('application_seq')").getSingleResult()).longValue();
    }
}
