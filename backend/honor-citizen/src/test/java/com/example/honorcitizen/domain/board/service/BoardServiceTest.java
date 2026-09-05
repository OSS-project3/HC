package com.example.honorcitizen.domain.board.service;

import com.example.honorcitizen.common.enums.BoardSearchType;
import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.board.dto.BoardCreateRequest;
import com.example.honorcitizen.domain.board.dto.BoardCreateResponse;
import com.example.honorcitizen.domain.board.dto.BoardDetailResponse;
import com.example.honorcitizen.domain.board.dto.BoardListItemResponse;
import com.example.honorcitizen.domain.board.dto.BoardUpdateRequest;
import com.example.honorcitizen.domain.board.entity.Board;
import com.example.honorcitizen.domain.board.entity.BoardAttachment;
import com.example.honorcitizen.domain.board.repository.BoardAttachmentRepository;
import com.example.honorcitizen.domain.board.repository.BoardRepository;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class BoardServiceTest {

    @Autowired
    private BoardService boardService;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private BoardAttachmentRepository boardAttachmentRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;

    @MockitoBean
    private StorageService storageService;

    private static final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        boardAttachmentRepository.deleteAll();
        uploadFileRepository.deleteAll();
        boardRepository.deleteAll();

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
        when(storageService.generatePresignedUrl(anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocation -> "https://mock-storage/" + invocation.getArgument(0));
    }

    private BoardCreateRequest request(BoardType boardType) {
        return new BoardCreateRequest(boardType, "제목", "본문");
    }

    private MockMultipartFile pdfFile(String name) {
        return new MockMultipartFile("attachments", name, "application/pdf", "dummy-pdf-content".getBytes());
    }

    @Test
    void createsNoticeWithAttachments() {
        BoardCreateResponse response = boardService.create(
                ADMIN_USER_ID, request(BoardType.NOTICE), List.of(pdfFile("a.pdf"), pdfFile("b.pdf")));

        Board board = boardRepository.findById(response.getId()).orElseThrow();
        assertThat(board.getBoardType()).isEqualTo(BoardType.NOTICE);
        assertThat(board.getCreatedByUserId()).isEqualTo(ADMIN_USER_ID);

        List<BoardAttachment> attachments = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(board.getId());
        assertThat(attachments).hasSize(2);
        assertThat(attachments.get(0).getDisplayOrder()).isZero();
        assertThat(attachments.get(1).getDisplayOrder()).isOne();
        assertThat(uploadFileRepository.count()).isEqualTo(2);
    }

    @Test
    void createsFaqWithoutAttachments() {
        BoardCreateResponse response = boardService.create(ADMIN_USER_ID, request(BoardType.FAQ), List.of());

        Board board = boardRepository.findById(response.getId()).orElseThrow();
        assertThat(board.getBoardType()).isEqualTo(BoardType.FAQ);
        assertThat(boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(board.getId())).isEmpty();
    }

    @Test
    void rejectsFaqWithAttachments() {
        assertThatThrownBy(() -> boardService.create(ADMIN_USER_ID, request(BoardType.FAQ), List.of(pdfFile("a.pdf"))))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThat(boardRepository.count()).isZero();
        assertThat(uploadFileRepository.count()).isZero();
    }

    @Test
    void rejectsMoreThanTenAttachments() {
        List<MockMultipartFile> files = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> pdfFile(i + ".pdf"))
                .toList();

        assertThatThrownBy(() -> boardService.create(ADMIN_USER_ID, request(BoardType.NOTICE), List.copyOf(files)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void listFiltersByBoardTypeAndSortsByCreatedAtDesc() {
        boardRepository.save(Board.create(BoardType.NOTICE, "공지1", "내용1", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.NOTICE, "공지2", "내용2", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.FAQ, "질문1", "답변1", ADMIN_USER_ID));

        PageResponse<BoardListItemResponse> result = boardService.list(BoardType.NOTICE, null, null, 0, 9);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(BoardListItemResponse::getTitle)
                .containsExactly("공지2", "공지1");
    }

    @Test
    void listRejectsInvalidPagingParams() {
        assertThatThrownBy(() -> boardService.list(BoardType.NOTICE, null, null, -1, 9))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> boardService.list(BoardType.NOTICE, null, null, 0, 0))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> boardService.list(BoardType.NOTICE, null, null, 0, 101))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> boardService.list(null, null, null, 0, 9))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    // 공지 서버 검색(2026-09-05 신규) — ReviewSpecifications 패턴 재사용. Board는 authorDisplayName이
    // 없어 title/content 두 컬럼만 대상이다(BoardSearchType 참고).
    @Test
    void listFiltersByTitleKeywordOnly() {
        boardRepository.save(Board.create(BoardType.NOTICE, "여름방학 휴무 안내", "본문에는 다른 단어만 있습니다", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.NOTICE, "정기 점검 안내", "여름방학이라는 단어는 본문에도 없습니다", ADMIN_USER_ID));

        PageResponse<BoardListItemResponse> result =
                boardService.list(BoardType.NOTICE, BoardSearchType.TITLE, "여름방학", 0, 9);

        assertThat(result.getContent()).extracting(BoardListItemResponse::getTitle)
                .containsExactly("여름방학 휴무 안내");
    }

    @Test
    void listFiltersByContentKeywordOnly() {
        boardRepository.save(Board.create(BoardType.NOTICE, "공지1", "여름방학 휴무 안내입니다", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.NOTICE, "여름방학 공지2", "다른 내용입니다", ADMIN_USER_ID));

        PageResponse<BoardListItemResponse> result =
                boardService.list(BoardType.NOTICE, BoardSearchType.CONTENT, "여름방학", 0, 9);

        assertThat(result.getContent()).extracting(BoardListItemResponse::getTitle)
                .containsExactly("공지1");
    }

    @Test
    void listWithoutSearchTypeSearchesBothTitleAndContent() {
        boardRepository.save(Board.create(BoardType.NOTICE, "여름방학 공지", "내용1", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.NOTICE, "공지2", "여름방학 관련 내용", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.NOTICE, "무관한 공지3", "무관한 내용3", ADMIN_USER_ID));

        PageResponse<BoardListItemResponse> result =
                boardService.list(BoardType.NOTICE, null, "여름방학", 0, 9);

        assertThat(result.getContent()).extracting(BoardListItemResponse::getTitle)
                .containsExactlyInAnyOrder("여름방학 공지", "공지2");
    }

    @Test
    void listSearchDoesNotCrossBoardTypeBoundary() {
        boardRepository.save(Board.create(BoardType.NOTICE, "여름방학 공지", "내용", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.FAQ, "여름방학 질문", "답변", ADMIN_USER_ID));

        PageResponse<BoardListItemResponse> result =
                boardService.list(BoardType.NOTICE, null, "여름방학", 0, 9);

        assertThat(result.getContent()).extracting(BoardListItemResponse::getTitle)
                .containsExactly("여름방학 공지");
    }

    @Test
    void listWithBlankKeywordReturnsAllInType() {
        boardRepository.save(Board.create(BoardType.NOTICE, "공지1", "내용1", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.NOTICE, "공지2", "내용2", ADMIN_USER_ID));

        PageResponse<BoardListItemResponse> result = boardService.list(BoardType.NOTICE, BoardSearchType.TITLE, " ", 0, 9);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void detailReturnsAttachmentsAndNextWithinSameBoardType() {
        Board older = boardRepository.save(Board.create(BoardType.NOTICE, "오래된 공지", "내용", ADMIN_USER_ID));
        BoardCreateResponse created = boardService.create(
                ADMIN_USER_ID, request(BoardType.NOTICE), List.of(pdfFile("a.pdf")));
        // FAQ는 같은 id 범위라도 next 후보에서 제외되어야 한다.
        boardRepository.save(Board.create(BoardType.FAQ, "관련없는 FAQ", "내용", ADMIN_USER_ID));

        BoardDetailResponse detail = boardService.detail(created.getId());

        assertThat(detail.getAttachments()).hasSize(1);
        assertThat(detail.getAttachments().get(0).getOriginalFileName()).isEqualTo("a.pdf");
        assertThat(detail.getNext()).isNotNull();
        assertThat(detail.getNext().getId()).isEqualTo(older.getId());
    }

    @Test
    void detailNotFoundThrows() {
        assertThatThrownBy(() -> boardService.detail(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    void updateOverwritesFields() {
        Board board = boardRepository.save(Board.create(BoardType.NOTICE, "원래 제목", "원래 내용", ADMIN_USER_ID));

        boardService.update(board.getId(), new BoardUpdateRequest(BoardType.FAQ, "새 제목", "새 내용", null), null);

        Board updated = boardRepository.findById(board.getId()).orElseThrow();
        assertThat(updated.getBoardType()).isEqualTo(BoardType.FAQ);
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(updated.getContent()).isEqualTo("새 내용");
        assertThat(updated.getCreatedByUserId()).isEqualTo(ADMIN_USER_ID);
    }

    @Test
    void updateNotFoundThrows() {
        assertThatThrownBy(() -> boardService.update(999L, new BoardUpdateRequest(BoardType.NOTICE, "t", "c", null), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    void updateKeepsSelectedAttachmentsAndRemovesOthers() {
        BoardCreateResponse created = boardService.create(
                ADMIN_USER_ID, request(BoardType.NOTICE), List.of(pdfFile("a.pdf"), pdfFile("b.pdf"), pdfFile("c.pdf")));
        List<BoardAttachment> before = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId());
        Long keepA = before.get(0).getId();
        Long keepC = before.get(2).getId();

        boardService.update(created.getId(),
                new BoardUpdateRequest(BoardType.NOTICE, "제목", "내용", List.of(keepA, keepC)), null);

        List<BoardAttachment> after = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId());
        assertThat(after).hasSize(2);
        assertThat(after.get(0).getUploadFileId()).isEqualTo(before.get(0).getUploadFileId());
        assertThat(after.get(1).getUploadFileId()).isEqualTo(before.get(2).getUploadFileId());
        assertThat(after.get(0).getDisplayOrder()).isZero();
        assertThat(after.get(1).getDisplayOrder()).isOne();
        assertThat(uploadFileRepository.count()).isEqualTo(2);
        verify(storageService, times(1)).delete(anyString());
    }

    @Test
    void updateAddsNewAttachmentsAlongsideKept() {
        BoardCreateResponse created = boardService.create(
                ADMIN_USER_ID, request(BoardType.NOTICE), List.of(pdfFile("a.pdf")));
        Long keepA = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId()).get(0).getId();

        boardService.update(created.getId(),
                new BoardUpdateRequest(BoardType.NOTICE, "제목", "내용", List.of(keepA)), List.of(pdfFile("new.pdf")));

        List<BoardAttachment> after = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId());
        assertThat(after).hasSize(2);
        assertThat(after.get(0).getDisplayOrder()).isZero();
        assertThat(after.get(1).getDisplayOrder()).isOne();
        assertThat(uploadFileRepository.count()).isEqualTo(2);
        verify(storageService, times(0)).delete(anyString());
    }

    @Test
    void updateRemovesAllAttachmentsWhenKeepListOmitted() {
        BoardCreateResponse created = boardService.create(
                ADMIN_USER_ID, request(BoardType.NOTICE), List.of(pdfFile("a.pdf"), pdfFile("b.pdf")));

        boardService.update(created.getId(), new BoardUpdateRequest(BoardType.NOTICE, "제목", "내용", null), null);

        assertThat(boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId())).isEmpty();
        assertThat(uploadFileRepository.count()).isZero();
        verify(storageService, times(2)).delete(anyString());
    }

    @Test
    void updateRejectsFaqWithKeptAttachments() {
        BoardCreateResponse created = boardService.create(
                ADMIN_USER_ID, request(BoardType.NOTICE), List.of(pdfFile("a.pdf")));
        Long keepA = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId()).get(0).getId();

        assertThatThrownBy(() -> boardService.update(created.getId(),
                new BoardUpdateRequest(BoardType.FAQ, "제목", "내용", List.of(keepA)), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        // 검증 실패는 커밋 전이라 기존 첨부파일이 그대로 남아 있어야 한다.
        assertThat(boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId())).hasSize(1);
    }

    @Test
    void updateRejectsWhenKeptPlusNewExceedsTen() {
        BoardCreateResponse created = boardService.create(
                ADMIN_USER_ID, request(BoardType.NOTICE), List.of(pdfFile("a.pdf"), pdfFile("b.pdf")));
        List<BoardAttachment> existing = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId());
        List<Long> keepIds = existing.stream().map(BoardAttachment::getId).toList();

        List<MockMultipartFile> newFiles = java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> pdfFile(i + ".pdf"))
                .toList();

        assertThatThrownBy(() -> boardService.update(created.getId(),
                new BoardUpdateRequest(BoardType.NOTICE, "제목", "내용", keepIds), List.copyOf(newFiles)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void deleteRemovesBoardAttachmentsAndUploadFiles() {
        BoardCreateResponse created = boardService.create(
                ADMIN_USER_ID, request(BoardType.NOTICE), List.of(pdfFile("a.pdf"), pdfFile("b.pdf")));

        boardService.delete(created.getId());

        assertThat(boardRepository.findById(created.getId())).isEmpty();
        assertThat(boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(created.getId())).isEmpty();
        assertThat(uploadFileRepository.count()).isZero();
        verify(storageService, times(2)).delete(anyString());
    }

    @Test
    void deleteNotFoundThrows() {
        assertThatThrownBy(() -> boardService.delete(999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }
}
