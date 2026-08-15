package com.example.honorcitizen.domain.event.service;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
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
        return new EventCreateRequest(eventType, "서울공예트렌드페어", LocalDate.of(2026, 12, 1), "2026. 12",
                "서울 코엑스 Hall C", "한국공예·디자인문화진흥원", "명예한국인증 · 방문증", "부스를 찾은 방문객에게...", null, null);
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
                request(EventType.BOOTH), imageFile("thumb.webp"), List.of(imageFile("a.webp"), imageFile("b.webp")));

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
        EventCreateResponse response = eventService.create(request(EventType.BOOTH), null, List.of());

        EventPost eventPost = eventPostRepository.findById(response.getId()).orElseThrow();
        assertThat(eventPost.getThumbnailImagePath()).isNull();
        assertThat(eventImageRepository.findByEventPostIdOrderByDisplayOrderAsc(eventPost.getId())).isEmpty();
    }

    @Test
    void rejectsMoreThanTenImages() {
        List<MockMultipartFile> files = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> imageFile(i + ".webp"))
                .toList();

        assertThatThrownBy(() -> eventService.create(request(EventType.BOOTH), null, List.copyOf(files)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThat(eventPostRepository.count()).isZero();
    }

    @Test
    void listReturnsOnlyVisiblePostsOfRequestedType() {
        eventPostRepository.save(EventPost.create(EventType.BOOTH, "공개1", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, true, null));
        eventPostRepository.save(EventPost.create(EventType.BOOTH, "비공개", null, "2026. 02",
                "장소", "주최", "카드", "내용", null, false, null));
        eventPostRepository.save(EventPost.create(EventType.COLLABORATION, "협업", null, "2026. 03",
                "장소", "주최", "카드", "내용", null, true, null));

        PageResponse<EventListItemResponse> result = eventService.list(EventType.BOOTH, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공개1");
    }

    @Test
    void listOrdersByDisplayOrderThenEventDateDescThenCreatedAtDesc() {
        eventPostRepository.save(EventPost.create(EventType.BOOTH, "A(순서1)", LocalDate.of(2026, 1, 1), "2026. 01",
                "장소", "주최", "카드", "내용", null, true, 1));
        eventPostRepository.save(EventPost.create(EventType.BOOTH, "B(순서0)", LocalDate.of(2026, 6, 1), "2026. 06",
                "장소", "주최", "카드", "내용", null, true, 0));
        eventPostRepository.save(EventPost.create(EventType.BOOTH, "C(순서없음,날짜있음)", LocalDate.of(2026, 12, 1), "2026. 12",
                "장소", "주최", "카드", "내용", null, true, null));
        eventPostRepository.save(EventPost.create(EventType.BOOTH, "D(순서없음,날짜없음)", null, "2026. 08",
                "장소", "주최", "카드", "내용", null, true, null));

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
                request(EventType.BOOTH), imageFile("thumb.webp"), List.of(imageFile("a.webp")));

        EventDetailResponse detail = eventService.detail(created.getId());

        assertThat(detail.getThumbnailImageUrl()).isNotNull();
        assertThat(detail.getImages()).hasSize(1);
        assertThat(detail.getImages().get(0).getOriginalFileName()).isEqualTo("a.webp");
    }

    @Test
    void detailThrowsNotFoundForHiddenPost() {
        EventPost hidden = eventPostRepository.save(EventPost.create(EventType.BOOTH, "비공개", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, false, null));

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

    @Test
    void updateOverwritesFields() {
        EventPost eventPost = eventPostRepository.save(EventPost.create(EventType.BOOTH, "원래 제목", null, "2026. 01",
                "원래 장소", "원래 주최", "원래 카드", "원래 내용", null, true, null));

        eventService.update(eventPost.getId(), new EventUpdateRequest(EventType.COLLABORATION, "새 제목",
                LocalDate.of(2026, 5, 1), "2026. 05", "새 장소", "새 주최", "새 카드", "새 내용", false, 3), null);

        EventPost updated = eventPostRepository.findById(eventPost.getId()).orElseThrow();
        assertThat(updated.getEventType()).isEqualTo(EventType.COLLABORATION);
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(updated.isVisible()).isFalse();
        assertThat(updated.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void updateReplacesThumbnailAndDeletesOldAfterCommit() {
        EventCreateResponse created = eventService.create(request(EventType.BOOTH), imageFile("old.webp"), List.of());
        EventPost before = eventPostRepository.findById(created.getId()).orElseThrow();
        String oldPath = before.getThumbnailImagePath();

        eventService.update(created.getId(), new EventUpdateRequest(EventType.BOOTH, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", true, null), imageFile("new.webp"));

        EventPost after = eventPostRepository.findById(created.getId()).orElseThrow();
        assertThat(after.getThumbnailImagePath()).isNotEqualTo(oldPath);
        verify(storageService, times(1)).delete(oldPath);
    }

    @Test
    void updateNotFoundThrows() {
        EventUpdateRequest request = new EventUpdateRequest(EventType.BOOTH, "제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", true, null);

        assertThatThrownBy(() -> eventService.update(999L, request, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND);
    }

    @Test
    void deleteRemovesEventPostAndImagesAndCleansUpStorage() {
        EventCreateResponse created = eventService.create(
                request(EventType.BOOTH), imageFile("thumb.webp"), List.of(imageFile("a.webp"), imageFile("b.webp")));

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
