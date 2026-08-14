package com.example.honorcitizen.domain.board.service;

import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.common.enums.UploadFileType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.board.dto.BoardAttachmentResponse;
import com.example.honorcitizen.domain.board.dto.BoardCreateRequest;
import com.example.honorcitizen.domain.board.dto.BoardCreateResponse;
import com.example.honorcitizen.domain.board.dto.BoardDetailResponse;
import com.example.honorcitizen.domain.board.dto.BoardListItemResponse;
import com.example.honorcitizen.domain.board.dto.BoardUpdateRequest;
import com.example.honorcitizen.domain.board.entity.Board;
import com.example.honorcitizen.domain.board.entity.BoardAttachment;
import com.example.honorcitizen.domain.board.repository.BoardAttachmentRepository;
import com.example.honorcitizen.domain.board.repository.BoardRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private static final int MAX_PAGE_SIZE = 100;
    // 첨부파일은 게시글 상세에 상시 노출되므로 Review 이미지와 동일하게 짧게 둘 이유가 없다.
    private static final long ATTACHMENT_URL_EXPIRY_SECONDS = 60 * 60L;

    private final BoardRepository boardRepository;
    private final BoardAttachmentRepository boardAttachmentRepository;
    private final UploadFileRepository uploadFileRepository;
    private final BoardAttachmentValidator attachmentValidator;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public PageResponse<BoardListItemResponse> list(BoardType type, int page, int size) {
        if (type == null || page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // createdAt만으로는 동시 등록 시 밀리초 단위로 값이 같아질 수 있어 id를 2차 정렬키로 더한다(Review와 동일 이유).
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Board> boards = boardRepository.findByBoardType(type, pageable);
        return PageResponse.from(boards, BoardListItemResponse::from);
    }

    @Transactional(readOnly = true)
    public BoardDetailResponse detail(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        List<BoardAttachmentResponse> attachments = loadAttachments(id);

        BoardDetailResponse.NextBoard next = boardRepository
                .findFirstByIdLessThanAndBoardTypeOrderByIdDesc(id, board.getBoardType())
                .map(BoardDetailResponse.NextBoard::from)
                .orElse(null);

        return BoardDetailResponse.of(board, attachments, next);
    }

    // 게시글 생성(data-model.md §4.1): S3 업로드 → Board/UploadFile/BoardAttachment 저장을 한 트랜잭션에서
    // 처리한다. Review.create()와 동일하게 파일 수가 적어(최대 10개) 트랜잭션 안에서 업로드해도 커넥션
    // 점유 시간이 문제되지 않는다 — Application처럼 업로드/영속을 별도 서비스로 분리하지 않는다.
    @Transactional
    public BoardCreateResponse create(Long adminUserId, BoardCreateRequest request, List<MultipartFile> attachments) {
        List<MultipartFile> files = presentFiles(attachments);
        validateAttachmentRequest(request.getBoardType(), files);
        files.forEach(attachmentValidator::validate);

        List<String> uploadedKeys = new ArrayList<>();
        try {
            Board board = boardRepository.save(
                    Board.create(request.getBoardType(), request.getTitle(), request.getContent(), adminUserId));

            int displayOrder = 0;
            for (MultipartFile file : files) {
                UploadFile uploadFile = uploadAttachment(file, uploadedKeys);
                boardAttachmentRepository.save(BoardAttachment.create(board.getId(), uploadFile.getId(), displayOrder++));
            }

            return BoardCreateResponse.from(board);
        } catch (RuntimeException e) {
            deleteUploadedFilesQuietlyReversed(uploadedKeys);
            throw e;
        }
    }

    // 전체 재제출(api.md §API 4) — boardType/title/content만 갱신한다. 첨부파일 편집은 이번 패스 범위 밖.
    @Transactional
    public void update(Long id, BoardUpdateRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        board.update(request.getBoardType(), request.getTitle(), request.getContent());
    }

    // 게시글 삭제(data-model.md §4.4): BoardAttachment·UploadFile을 Board와 함께 한 트랜잭션에서 지우고,
    // S3 객체는 커밋 이후 삭제한다 — 순서를 바꾸면 롤백 시 DB엔 남아있는데 S3만 사라지는 불일치가 생긴다.
    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        List<BoardAttachment> attachments = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(id);
        List<UploadFile> uploadFiles = uploadFileRepository.findAllById(
                attachments.stream().map(BoardAttachment::getUploadFileId).toList());

        boardAttachmentRepository.deleteByBoardId(id);
        uploadFileRepository.deleteAll(uploadFiles);
        boardRepository.delete(board);

        List<String> keys = uploadFiles.stream().map(UploadFile::getFilePath).toList();
        deleteFilesAfterCommit(keys);
    }

    private List<BoardAttachmentResponse> loadAttachments(Long boardId) {
        List<BoardAttachment> attachments = boardAttachmentRepository.findByBoardIdOrderByDisplayOrderAsc(boardId);
        if (attachments.isEmpty()) {
            return List.of();
        }

        Map<Long, UploadFile> uploadFileById = uploadFileRepository
                .findAllById(attachments.stream().map(BoardAttachment::getUploadFileId).toList())
                .stream()
                .collect(Collectors.toMap(UploadFile::getId, Function.identity()));

        return attachments.stream()
                .map(attachment -> {
                    UploadFile uploadFile = uploadFileById.get(attachment.getUploadFileId());
                    String url = storageService.generatePresignedUrl(uploadFile.getFilePath(), ATTACHMENT_URL_EXPIRY_SECONDS);
                    return BoardAttachmentResponse.of(attachment.getId(), uploadFile.getOriginalName(), url);
                })
                .toList();
    }

    // FAQ는 첨부파일 개념 자체가 없어 거절한다(api.md §API 3 Validation, 2026-08-14 확정).
    private void validateAttachmentRequest(BoardType boardType, List<MultipartFile> files) {
        if (boardType == BoardType.FAQ && !files.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (files.size() > BoardAttachmentValidator.MAX_ATTACHMENT_COUNT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    // 컨트롤러가 빈 파트를 그대로 넘길 수 있어(예: 브라우저가 파일 없이도 빈 MultipartFile을 보냄) 실제
    // 내용이 있는 파일만 걸러낸다 — Review의 singleImageOrNull과 동일한 이유.
    private List<MultipartFile> presentFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream().filter(file -> !file.isEmpty()).toList();
    }

    private UploadFile uploadAttachment(MultipartFile file, List<String> uploadedKeys) {
        String storedName = UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        String key = "boards/attachments/" + storedName;
        storageService.upload(key, file);
        uploadedKeys.add(key);

        UploadFile uploadFile = UploadFile.create(file.getOriginalFilename(), storedName, key,
                UploadFileType.BOARD_ATTACHMENT, file.getContentType(), file.getSize());
        return uploadFileRepository.save(uploadFile);
    }

    private void deleteFilesAfterCommit(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            keys.forEach(this::deleteFileQuietly);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(BoardService.this::deleteFileQuietly);
            }
        });
    }

    // 역순 삭제 이유(ApplicationService.deleteUploadedFilesQuietlyReversed와 동일 정책):
    // 파일 간 의존 관계가 생기더라도 의존하는 파일을 먼저 지워야 고아 참조가 남지 않는다.
    private void deleteUploadedFilesQuietlyReversed(List<String> uploadedKeys) {
        for (int i = uploadedKeys.size() - 1; i >= 0; i--) {
            deleteFileQuietly(uploadedKeys.get(i));
        }
    }

    private void deleteFileQuietly(String key) {
        try {
            storageService.delete(key);
        } catch (RuntimeException e) {
            log.warn("Failed to delete board attachment from storage. key={}", key, e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
