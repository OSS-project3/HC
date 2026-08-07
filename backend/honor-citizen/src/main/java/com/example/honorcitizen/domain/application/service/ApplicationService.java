package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UploadFileType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.enums.LookupMethod;
import com.example.honorcitizen.domain.application.dto.ApplicationCardDownloadResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationPhotoReuploadResponse;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final long CARD_DOWNLOAD_URL_EXPIRY_SECONDS = 7 * 24 * 60 * 60L;

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final CardTypeRepository cardTypeRepository;
    private final UploadFileRepository uploadFileRepository;
    private final UserService userService;
    private final ApplicationPersistenceService applicationPersistenceService;
    private final ApplicationPhotoValidator applicationPhotoValidator;
    private final StorageService storageService;
    private final BulkExcelParser bulkExcelParser;

    public ApplicationCreateResponse createIndividual(Long userId, ApplicationCreateRequest request,
            MultipartFile photo, MultipartFile schoolLogo, MultipartFile schoolSeal) {
        User user = findUser(userId);
        CardType cardType = findActiveCardType(request.getCardTypeId());
        boolean isStudent = cardType.isStudentCard();

        validateCreateIndividual(request, photo, schoolLogo, schoolSeal, isStudent);

        String applicationNumber = generateApplicationNumber();
        Long logoFileId = isStudent ? storeUploadFile(schoolLogo, UploadFileType.PHOTO) : null;
        Long sealFileId = isStudent && isPresent(schoolSeal) ? storeUploadFile(schoolSeal, UploadFileType.PHOTO) : null;
        boolean receiverSameAsApplicant = request.isReceiverSameAsApplicant();
        String photoPath = storePhotoFile(applicationNumber, photo);

        Application application = applicationPersistenceService.saveIndividual(
                userId, applicationNumber, cardType.getId(), request.getIssueType(), receiverSameAsApplicant,
                logoFileId, sealFileId, request, user.getEmail(), photoPath);

        return ApplicationCreateResponse.from(application);
    }

    private void validateCreateIndividual(ApplicationCreateRequest request, MultipartFile photo,
            MultipartFile schoolLogo, MultipartFile schoolSeal, boolean isStudent) {
        validateReceiverPresence(request);
        applicationPhotoValidator.validateFacePhoto(photo);
        validateStudentFields(isStudent, request.getMember().getStudentId(), request.getMember().getDepartment(),
                schoolLogo, schoolSeal);
    }

    private User findUser(Long userId) {
        return userService.findEligibleApplicationUser(userId);
    }

    public BulkApplicationCreateResponse createGroup(Long userId, BulkApplicationCreateRequest request,
            MultipartFile logo, MultipartFile seal, MultipartFile submitFile) {
        CardType cardType = findActiveCardType(request.getCardTypeId());
        boolean isStudent = cardType.isStudentCard();

        validateGroupReceiverPresence(request);
        if (!isPresent(logo) || (!isStudent && !isPresent(seal))) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<BulkMemberRow> rows = bulkExcelParser.parse(submitFile, isStudent);

        String applicationNumber = generateApplicationNumber();
        Long logoFileId = storeUploadFile(logo, UploadFileType.PHOTO);
        Long sealFileId = isPresent(seal) ? storeUploadFile(seal, UploadFileType.PHOTO) : null;
        Long submitFileId = storeUploadFile(submitFile, UploadFileType.ZIP);
        boolean receiverSameAsApplicant = request.getReceiver() == null || request.getReceiver().isSameAsApplicant();

        List<GroupMemberUpload> memberUploads = rows.stream()
                .map(row -> new GroupMemberUpload(row, storePhotoBytes(applicationNumber, row.photoFilename(), row.photoBytes())))
                .toList();

        User user = userService.findById(userId);

        Application application = applicationPersistenceService.saveGroup(
                userId, applicationNumber, cardType.getId(), request.getIssueType(), receiverSameAsApplicant,
                rows.size(), logoFileId, sealFileId, submitFileId, request, user.getEmail(), memberUploads);

        return BulkApplicationCreateResponse.from(application);
    }

    @Transactional
    public ApplicationPhotoReuploadResponse reuploadPhoto(Long userId, Long applicationId,
            MultipartFile photo, MultipartFile submitFile) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (application.getStatus() != ApplicationStatus.PHOTO_REJECTED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        if (application.isIndividual()) {
            if (!isPresent(photo) || isPresent(submitFile)) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            ApplicationMember member = applicationMemberRepository.findByApplicationId(applicationId).get(0);
            member.updatePhoto(storePhotoFile(application.getApplicationNumber(), photo));
            application.resubmitForReview(null);
        } else {
            if (!isPresent(submitFile) || isPresent(photo)) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            CardType cardType = cardTypeRepository.findById(application.getCardTypeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
            List<BulkMemberRow> rows = bulkExcelParser.parse(submitFile, cardType.isStudentCard());

            Long newSubmitFileId = storeUploadFile(submitFile, UploadFileType.ZIP);
            applicationMemberRepository.deleteByApplicationId(applicationId);
            for (BulkMemberRow row : rows) {
                String photoPath = storePhotoBytes(application.getApplicationNumber(), row.photoFilename(), row.photoBytes());
                ApplicationMember member = ApplicationMember.createGroupRow(
                        applicationId, row.englishName(), row.birthDate(), row.nationality(),
                        row.birthTime(), row.birthRegion(), row.gender(), row.entryDate(),
                        row.email(), row.phone(), row.address(), row.studentId(), row.department(), photoPath);
                applicationMemberRepository.save(member);
            }
            application.updateTotalQuantity(rows.size());
            application.resubmitForReview(newSubmitFileId);
        }

        return ApplicationPhotoReuploadResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ApplicationCardDownloadResponse getCardDownload(Long userId, Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!application.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
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

        String downloadUrl = buildGroupCardsZipAndGetUrl(application, members);
        return ApplicationCardDownloadResponse.forGroup(applicationId, downloadUrl, expiresAt);
    }

    private String buildGroupCardsZipAndGetUrl(Application application, List<ApplicationMember> members) {
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
            int index = 1;
            for (ApplicationMember member : members) {
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

    @Transactional(readOnly = true)
    public ApplicationLookupResponse lookup(ApplicationLookupRequest request) {
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
                application.getApplicationNumber(),
                maskName(applicant.getName()),
                cardType.getName(),
                application.getStatus(),
                application.getPhotoRejectReason(),
                application.getCreatedAt());
    }

    private Application lookupByCard(ApplicationLookupRequest request) {
        ApplicationMember member = applicationMemberRepository.findByCardNumber(request.getKeyValue())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        return applicationRepository.findById(member.getApplicationId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private Application lookupByApplicationNumber(ApplicationLookupRequest request) {
        Application application = applicationRepository.findByApplicationNumber(request.getKeyValue())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Applicant applicant = applicantRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        if (!matches(request, applicant.getPhone(), applicant.getEmail())) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return application;
    }

    private boolean matches(ApplicationLookupRequest request, String targetPhone, String targetEmail) {
        boolean phoneMatches = request.getPhone() != null && request.getPhone().equals(targetPhone);
        boolean emailMatches = request.getEmail() != null && request.getEmail().equalsIgnoreCase(targetEmail);
        return phoneMatches && emailMatches;
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    private void validateGroupReceiverPresence(BulkApplicationCreateRequest request) {
        if (request.getIssueType() == IssueType.MOBILE_AND_PHYSICAL && request.getReceiver() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.getIssueType() == IssueType.MOBILE && request.getReceiver() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private String storePhotoBytes(String applicationNumber, String originalFilename, byte[] bytes) {
        String key = "applications/" + applicationNumber + "/member-photos/" + UUID.randomUUID() + "-"
                + sanitizeFilename(originalFilename);
        storageService.uploadBytes(key, bytes, guessContentType(originalFilename));
        return key;
    }

    private String guessContentType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private CardType findActiveCardType(Long cardTypeId) {
        return cardTypeRepository.findById(cardTypeId)
                .filter(CardType::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private void validateReceiverPresence(ApplicationCreateRequest request) {
        if (request.getIssueType() == IssueType.MOBILE_AND_PHYSICAL && request.getReceiver() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (request.getIssueType() == IssueType.MOBILE && request.getReceiver() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateStudentFields(boolean isStudent, String studentId, String department,
            MultipartFile schoolLogo, MultipartFile schoolSeal) {
        boolean anyStudentFieldPresent = hasText(studentId) || hasText(department)
                || isPresent(schoolLogo) || isPresent(schoolSeal);
        boolean allRequiredStudentFieldsPresent = hasText(studentId) && hasText(department)
                && isPresent(schoolLogo);

        if (isStudent && !allRequiredStudentFieldsPresent) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (!isStudent && anyStudentFieldPresent) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (isStudent) {
            applicationPhotoValidator.validateSchoolAsset(schoolLogo);
            if (isPresent(schoolSeal)) {
                applicationPhotoValidator.validateSchoolAsset(schoolSeal);
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isPresent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String storePhotoFile(String applicationNumber, MultipartFile photo) {
        String key = "applications/" + applicationNumber + "/member-photos/" + UUID.randomUUID() + "-"
                + sanitizeFilename(photo.getOriginalFilename());
        storageService.upload(key, photo);
        return key;
    }

    private Long storeUploadFile(MultipartFile file, UploadFileType fileType) {
        String storedName = UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        String key = "applications/uploads/" + storedName;
        storageService.upload(key, file);

        UploadFile uploadFile = UploadFile.create(
                file.getOriginalFilename(), storedName, key, fileType, file.getContentType(), file.getSize());
        return uploadFileRepository.save(uploadFile).getId();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String generateApplicationNumber() {
        int year = LocalDate.now().getYear();
        String prefix = "APP-" + year + "-";
        long sequence = applicationRepository.countByApplicationNumberStartingWith(prefix) + 1;
        return prefix + String.format("%06d", sequence);
    }
}
