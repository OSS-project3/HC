package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.domain.board.entity.Board;
import com.example.honorcitizen.domain.board.repository.BoardAttachmentRepository;
import com.example.honorcitizen.domain.board.repository.BoardRepository;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private BoardAttachmentRepository boardAttachmentRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;

    private static final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        boardAttachmentRepository.deleteAll();
        uploadFileRepository.deleteAll();
        boardRepository.deleteAll();
    }

    @Test
    void listSucceedsWithoutAuthentication() throws Exception {
        boardRepository.save(Board.create(BoardType.NOTICE, "공지 제목", "공지 내용", ADMIN_USER_ID));

        mockMvc.perform(get("/api/boards").param("type", "NOTICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("공지 제목"));
    }

    @Test
    void listReturnsBadRequestWhenTypeMissing() throws Exception {
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void listReturnsBadRequestForInvalidType() throws Exception {
        mockMvc.perform(get("/api/boards").param("type", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    // 공지 서버 검색 HTTP 배선 확인 — 검색 로직 자체는 BoardServiceTest가 커버.
    @Test
    void listAppliesKeywordAndSearchTypeQueryParams() throws Exception {
        boardRepository.save(Board.create(BoardType.NOTICE, "여름방학 휴무 안내", "본문 내용", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.NOTICE, "정기 점검 안내", "여기엔 여름방학이 없습니다", ADMIN_USER_ID));

        mockMvc.perform(get("/api/boards")
                        .param("type", "NOTICE")
                        .param("searchType", "TITLE")
                        .param("keyword", "여름방학"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("여름방학 휴무 안내"));
    }

    @Test
    void detailSucceedsWithoutAuthentication() throws Exception {
        Board board = boardRepository.save(Board.create(BoardType.FAQ, "질문", "답변", ADMIN_USER_ID));

        mockMvc.perform(get("/api/boards/{id}", board.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("질문"))
                .andExpect(jsonPath("$.data.attachments").isArray())
                .andExpect(jsonPath("$.data.attachments").isEmpty());
    }

    @Test
    void detailReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/boards/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BOARD_NOT_FOUND"));
    }
}
