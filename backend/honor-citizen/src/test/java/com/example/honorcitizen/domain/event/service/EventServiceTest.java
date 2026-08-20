package com.example.honorcitizen.domain.event.service;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.event.dto.EventAdminDetailResponse;
import com.example.honorcitizen.domain.event.dto.EventAdminListItemResponse;
import com.example.honorcitizen.domain.event.dto.EventCreateRequest;
import com.example.honorcitizen.domain.event.dto.EventCreateResponse;
import com.example.honorcitizen.domain.event.dto.EventDetailResponse;
import com.example.honorcitizen.domain.event.dto.EventListItemResponse;
import com.example.honorcitizen.domain.event.dto.EventUpdateRequest;
import com.example.honorcitizen.domain.event.entity.EventImage;
import com.example.honorcitizen.domain.event.entity.EventPost;
import com.example.honorcitizen.domain.event.repository.EventImageRepository;
import com.example.honorcitizen.domain.event.repository.EventPostRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class EventServiceTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private EventPostRepository eventPostRepository;
    @Autowired
    private EventImageRepository eventImageRepository;

    @MockitoBean
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        eventImageRepository.deleteAll();
        eventPostRepository.deleteAll();

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
        when(storageService.generatePresignedUrl(anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> "https://mock-storage/" + invocation.getArgument(0));
    }

    private EventCreateRequest request(EventType eventType) {
        return request(eventType, null);
    }

    private EventCreateRequest request(EventType eventType, String companyName) {
        return new EventCreateRequest(eventType, "서울공예트렌드페어", LocalDate.of(2026, 12, 1), "2026. 12",
                "서울 코엑스 Hall C", "한국공예·디자인문화진흥원", "명예한국인증 · 방문증", "부스를 찾은 방문객에게...",
                companyName, null, null);
    }

    private EventPost savePost(EventType eventType, String title, LocalDate eventDate, String eventDateText,
            boolean visible, Integer displayOrder) {
        return eventPostRepository.save(EventPost.create(eventType, title, eventDate, eventDateText,
                "장소", "주최", "카드", "내용", null, null, null, visible, displayOrder));
    }

    // 10x10 단색 손실(lossy) webp — EventImageValidatorTest와 동일한 실제 유효 파일 바이너리(Pillow/libwebp 생성).
    private static final byte[] WEBP_10X10_RED = {
            82, 73, 70, 70, 60, 0, 0, 0, 87, 69, 66, 80, 86, 80, 56, 32, 48, 0, 0, 0, -48, 1, 0, -99, 1, 42, 10, 0,
            10, 0, 1, 64, 38, 37, -96, 2, 116, -70, 1, -8, 0, 3, -80, 0, -2, -14, -21, 127, -4, -40, 21, -51, 115,
            -17, -9, -1, -46, -32, -3, 46, 15, -46, -32, -1, -46, -112, 0, 0
    };

    private MockMultipartFile imageFile(String name) {
        return new MockMultipartFile("images", name, "image/webp", WEBP_10X10_RED);
    }

    @Test
    void createsEventWithThumbnailAndImages() {
        EventCreateResponse response = eventService.create(
                request(EventType.BOOTH), imageFile("thumb.webp"), null, List.of(imageFile("a.webp"), imageFile("b.webp")));

        EventPost eventPost = eventPostRepository.findById(response.getId()).orElseThrow();
        assertThat(eventPost.getEventType()).isEqualTo(EventType.BOOTH);
        assertThat(eventPost.getThumbnailImagePath()).isNotNull();
        assertThat(eventPost.isVisible()).isTrue();

        List<EventImage> images = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(eventPost.getId());
        assertThat(images).hasSize(2);
        assertThat(images.get(0).getDisplayOrder()).isZero();
        assertThat(images.get(1).getDisplayOrder()).isOne();
    }

    @Test
    void createsEventWithoutFiles() {
        EventCreateResponse response = eventService.create(request(EventType.BOOTH), null, null, List.of());

        EventPost eventPost = eventPostRepository.findById(response.getId()).orElseThrow();
        assertThat(eventPost.getThumbnailImagePath()).isNull();
        assertThat(eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(eventPost.getId())).isEmpty();
    }

    @Test
    void createsCollaborationEventWithCompanyNameAndLogo() {
        EventCreateResponse response = eventService.create(
                request(EventType.COLLABORATION, "OO기업"), null, imageFile("logo.webp"), List.of());

        EventPost eventPost = eventPostRepository.findById(response.getId()).orElseThrow();
        assertThat(eventPost.getCompanyName()).isEqualTo("OO기업");
        assertThat(eventPost.getLogoImagePath()).isNotNull();
    }

    @Test
    void createRejectsCompanyNameForBoothBeforeUploadingAnything() {
        assertThatThrownBy(() -> eventService.create(request(EventType.BOOTH, "OO기업"), null, null, List.of()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThat(eventPostRepository.count()).isZero();
    }

    @Test
    void createRejectsLogoForBoothBeforeUploadingAnything() {
        assertThatThrownBy(() -> eventService.create(request(EventType.BOOTH), null, imageFile("logo.webp"), List.of()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(storageService, org.mockito.Mockito.never()).upload(anyString(), any());
    }

    @Test
    void rejectsMoreThanTenImages() {
        List<MockMultipartFile> files = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> imageFile(i + ".webp"))
                .toList();

        assertThatThrownBy(() -> eventService.create(request(EventType.BOOTH), null, null, List.copyOf(files)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThat(eventPostRepository.count()).isZero();
    }

    @Test
    void listReturnsOnlyVisiblePostsOfRequestedType() {
        savePost(EventType.BOOTH, "공개1", null, "2026. 01", true, null);
        savePost(EventType.BOOTH, "비공개", null, "2026. 02", false, null);
        savePost(EventType.COLLABORATION, "협업", null, "2026. 03", true, null);

        PageResponse<EventListItemResponse> result = eventService.list(EventType.BOOTH, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공개1");
    }

    @Test
    void listOrdersByDisplayOrderThenEventDateDescThenCreatedAtDesc() {
        savePost(EventType.BOOTH, "A(순서1)", LocalDate.of(2026, 1, 1), "2026. 01", true, 1);
        savePost(EventType.BOOTH, "B(순서0)", LocalDate.of(2026, 6, 1), "2026. 06", true, 0);
        savePost(EventType.BOOTH, "C(순서없음,날짜있음)", LocalDate.of(2026, 12, 1), "2026. 12", true, null);
        savePost(EventType.BOOTH, "D(순서없음,날짜없음)", null, "2026. 08", true, null);

        PageResponse<EventListItemResponse> result = eventService.list(EventType.BOOTH, 0, 10);

        assertThat(result.getContent()).extracting(EventListItemResponse::getTitle)
                .containsExactly("B(순서0)", "A(순서1)", "C(순서없음,날짜있음)", "D(순서없음,날짜없음)");
    }

    @Test
    void listRejectsInvalidParams() {
        assertThatThrownBy(() -> eventService.list(null, 0, 10))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> eventService.list(EventType.BOOTH, -1, 10))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> eventService.list(EventType.BOOTH, 0, 0))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void detailReturnsThumbnailAndImages() {
        EventCreateResponse created = eventService.create(
                request(EventType.BOOTH), imageFile("thumb.webp"), null, List.of(imageFile("a.webp")));

        EventDetailResponse detail = eventService.detail(created.getId());

        assertThat(detail.getThumbnailImageUrl()).isNotNull();
        assertThat(detail.getImages()).hasSize(1);
        assertThat(detail.getImages().get(0).getOriginalFileName()).isEqualTo("a.webp");
    }

    @Test
    void detailThrowsNotFoundForHiddenPost() {
        EventPost hidden = savePost(EventType.BOOTH, "비공개", null, "2026. 01", false, null);

        assertThatThrownBy(() -> eventService.detail(hidden.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND);
    }

    @Test
    void detailNotFoundThrows() {
        assertThatThrownBy(() -> eventService.detail(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND);
    }

    // ── 관리자 목록·상세(EVENT-EXT-4) ──────────────────────────────────────

    @Test
    void listForAdminReturnsHiddenPostsToo() {
        savePost(EventType.BOOTH, "공개", null, "2026. 01", true, null);
        savePost(EventType.BOOTH, "비공개", null, "2026. 02", false, null);

        PageResponse<EventAdminListItemResponse> result = eventService.listForAdmin(EventType.BOOTH, null, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void listForAdminFiltersByVisibleWhenProvided() {
        savePost(EventType.BOOTH, "공개", null, "2026. 01", true, null);
        savePost(EventType.BOOTH, "비공개", null, "2026. 02", false, null);

        PageResponse<EventAdminListItemResponse> result = eventService.listForAdmin(null, false, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("비공개");
    }

    @Test
    void listForAdminWithoutFiltersReturnsAllTypesAndVisibility() {
        savePost(EventType.BOOTH, "부스", null, "2026. 01", true, null);
        savePost(EventType.COLLABORATION, "협업(숨김)", null, "2026. 02", false, null);

        PageResponse<EventAdminListItemResponse> result = eventService.listForAdmin(null, null, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void detailForAdminReturnsHiddenPost() {
        EventPost hidden = savePost(EventType.BOOTH, "비공개", null, "2026. 01", false, null);

        EventAdminDetailResponse detail = eventService.detailForAdmin(hidden.getId());

        assertThat(detail.getTitle()).isEqualTo("비공개");
        assertThat(detail.isVisible()).isFalse();
    }

    // ── 수정: 텍스트 필드·썸네일(EVENT-EXT-3) ──────────────────────────────

    @Test
    void updateOverwritesFields() {
        EventPost eventPost = savePost(EventType.BOOTH, "원래 제목", null, "2026. 01", true, null);

        eventService.update(eventPost.getId(), new EventUpdateRequest(EventType.COLLABORATION, "새 제목",
                LocalDate.of(2026, 5, 1), "2026. 05", "새 장소", "새 주최", "새 카드", "새 내용",
                "OO기업", true, false, null, false, 3), null, null, null);

        EventPost updated = eventPostRepository.findById(eventPost.getId()).orElseThrow();
        assertThat(updated.getEventType()).isEqualTo(EventType.COLLABORATION);
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(updated.getCompanyName()).isEqualTo("OO기업");
        assertThat(updated.isVisible()).isFalse();
        assertThat(updated.getDisplayOrder()).isEqualTo(3);
    }

    private EventUpdateRequest simpleUpdateRequest(EventType eventType, String companyName, Boolean removeLogo,
            Boolean removeThumbnail, List<Long> keepImageIds) {
        return new EventUpdateRequest(eventType, "제목", null, "2026. 01", "장소", "주최", "카드", "내용",
                companyName, removeLogo, removeThumbnail, keepImageIds, true, null);
    }

    @Test
    void updateReplacesThumbnailAndDeletesOldAfterCommit() {
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), imageFile("old.webp"), null, List.of());
        EventPost before = eventPostRepository.findById(created.getId()).orElseThrow();
        String oldPath = before.getThumbnailImagePath();

        eventService.update(created.getId(), simpleUpdateRequest(EventType.BOOTH, null, null, null, null),
                imageFile("new.webp"), null, null);

        EventPost after = eventPostRepository.findById(created.getId()).orElseThrow();
        assertThat(after.getThumbnailImagePath()).isNotEqualTo(oldPath);
        verify(storageService, times(1)).delete(oldPath);
    }

    @Test
    void updateRemoveThumbnailClearsPathAndDeletesOldAfterCommit() {
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), imageFile("old.webp"), null, List.of());
        EventPost before = eventPostRepository.findById(created.getId()).orElseThrow();
        String oldPath = before.getThumbnailImagePath();

        eventService.update(created.getId(), simpleUpdateRequest(EventType.BOOTH, null, null, true, null),
                null, null, null);

        EventPost after = eventPostRepository.findById(created.getId()).orElseThrow();
        assertThat(after.getThumbnailImagePath()).isNull();
        verify(storageService, times(1)).delete(oldPath);
    }

    @Test
    void updateRejectsRemoveThumbnailWithNewThumbnailTogether() {
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), imageFile("old.webp"), null, List.of());

        assertThatThrownBy(() -> eventService.update(created.getId(),
                simpleUpdateRequest(EventType.BOOTH, null, null, true, null), imageFile("new.webp"), null, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void updateNotFoundThrows() {
        EventUpdateRequest request = simpleUpdateRequest(EventType.BOOTH, null, null, null, null);

        assertThatThrownBy(() -> eventService.update(999L, request, null, null, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND);
    }

    // ── 수정: 협업 로고·companyName 불변조건(EVENT-EXT-3) ───────────────────

    @Test
    void updateReplacesLogoAndDeletesOldAfterCommit() {
        EventCreateResponse created = eventService.create(
                request(EventType.COLLABORATION, "OO기업"), null, imageFile("old-logo.webp"), List.of());
        String oldLogoPath = eventPostRepository.findById(created.getId()).orElseThrow().getLogoImagePath();

        eventService.update(created.getId(), simpleUpdateRequest(EventType.COLLABORATION, "OO기업", null, null, null),
                null, imageFile("new-logo.webp"), null);

        EventPost after = eventPostRepository.findById(created.getId()).orElseThrow();
        assertThat(after.getLogoImagePath()).isNotEqualTo(oldLogoPath);
        verify(storageService, times(1)).delete(oldLogoPath);
    }

    @Test
    void updateRejectsCompanyNameForBooth() {
        EventPost eventPost = savePost(EventType.BOOTH, "제목", null, "2026. 01", true, null);

        assertThatThrownBy(() -> eventService.update(eventPost.getId(),
                simpleUpdateRequest(EventType.BOOTH, "OO기업", null, null, null), null, null, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void updateRejectsNewLogoForBooth() {
        EventPost eventPost = savePost(EventType.BOOTH, "제목", null, "2026. 01", true, null);

        assertThatThrownBy(() -> eventService.update(eventPost.getId(),
                simpleUpdateRequest(EventType.BOOTH, null, null, null, null), null, imageFile("logo.webp"), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void updateRejectsCollaborationToBoothTransitionWithoutRemovingLogo() {
        EventCreateResponse created = eventService.create(
                request(EventType.COLLABORATION, "OO기업"), null, imageFile("logo.webp"), List.of());

        // BOOTH로 바꾸면서 companyName은 지웠지만 removeLogo를 안 보냄 — 기존 로고가 남아있어 거절돼야 한다.
        assertThatThrownBy(() -> eventService.update(created.getId(),
                simpleUpdateRequest(EventType.BOOTH, null, null, null, null), null, null, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void updateAllowsCollaborationToBoothTransitionWhenLogoRemoved() {
        EventCreateResponse created = eventService.create(
                request(EventType.COLLABORATION, "OO기업"), null, imageFile("logo.webp"), List.of());

        eventService.update(created.getId(), simpleUpdateRequest(EventType.BOOTH, null, true, null, null),
                null, null, null);

        EventPost after = eventPostRepository.findById(created.getId()).orElseThrow();
        assertThat(after.getEventType()).isEqualTo(EventType.BOOTH);
        assertThat(after.getCompanyName()).isNull();
        assertThat(after.getLogoImagePath()).isNull();
    }

    // ── 수정: 갤러리(keepImageIds) 편집(EVENT-EXT-3) ────────────────────────

    @Test
    void updateOmittingKeepImageIdsKeepsExistingGallery() {
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), null, null,
                List.of(imageFile("a.webp"), imageFile("b.webp")));

        eventService.update(created.getId(), simpleUpdateRequest(EventType.BOOTH, null, null, null, null),
                null, null, null);

        List<EventImage> images = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(created.getId());
        assertThat(images).hasSize(2);
    }

    @Test
    void updateWithEmptyKeepImageIdsDeletesEntireGallery() {
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), null, null,
                List.of(imageFile("a.webp"), imageFile("b.webp")));
        List<EventImage> before = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(created.getId());
        List<String> oldKeys = before.stream().map(EventImage::getImagePath).toList();

        eventService.update(created.getId(), simpleUpdateRequest(EventType.BOOTH, null, null, null, List.of()),
                null, null, null);

        assertThat(eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(created.getId())).isEmpty();
        oldKeys.forEach(key -> verify(storageService, times(1)).delete(key));
    }

    @Test
    void updateReordersKeptImagesAndAppendsNewOnes() {
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), null, null,
                List.of(imageFile("a.webp"), imageFile("b.webp")));
        List<EventImage> before = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(created.getId());
        Long firstId = before.get(0).getId();
        Long secondId = before.get(1).getId();

        // 순서를 뒤집어서 유지 + 신규 이미지 1장 추가.
        eventService.update(created.getId(),
                simpleUpdateRequest(EventType.BOOTH, null, null, null, List.of(secondId, firstId)),
                null, null, List.of(imageFile("c.webp")));

        List<EventImage> after = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(created.getId());
        assertThat(after).hasSize(3);
        assertThat(after.get(0).getId()).isEqualTo(secondId);
        assertThat(after.get(1).getId()).isEqualTo(firstId);
        assertThat(after.get(2).getOriginalFilename()).isEqualTo("c.webp");
    }

    @Test
    void updateRejectsKeepImageIdsFromAnotherEvent() {
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), null, null, List.of(imageFile("a.webp")));
        EventCreateResponse otherEvent = eventService.create(request(EventType.BOOTH), null, null, List.of(imageFile("x.webp")));
        Long foreignImageId = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(otherEvent.getId()).get(0).getId();

        assertThatThrownBy(() -> eventService.update(created.getId(),
                simpleUpdateRequest(EventType.BOOTH, null, null, null, List.of(foreignImageId)), null, null, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void updateRejectsWhenFinalGalleryExceedsTenImages() {
        List<MockMultipartFile> nineFiles = java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> imageFile(i + ".webp"))
                .toList();
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), null, null, List.copyOf(nineFiles));
        List<Long> keepAllIds = eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(created.getId())
                .stream().map(EventImage::getId).toList();

        assertThatThrownBy(() -> eventService.update(created.getId(),
                simpleUpdateRequest(EventType.BOOTH, null, null, null, keepAllIds),
                null, null, List.of(imageFile("new1.webp"), imageFile("new2.webp"))))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void deleteRemovesEventPostAndImagesAndCleansUpStorage() {
        EventCreateResponse created = eventService.create(
                request(EventType.BOOTH), imageFile("thumb.webp"), null, List.of(imageFile("a.webp"), imageFile("b.webp")));

        eventService.delete(created.getId());

        assertThat(eventPostRepository.findById(created.getId())).isEmpty();
        assertThat(eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(created.getId())).isEmpty();
        verify(storageService, times(3)).delete(anyString());
    }

    @Test
    void deleteNotFoundThrows() {
        assertThatThrownBy(() -> eventService.delete(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND);
    }
}
