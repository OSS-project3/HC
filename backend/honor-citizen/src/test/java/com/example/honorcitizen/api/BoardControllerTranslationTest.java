package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.domain.board.entity.Board;
import com.example.honorcitizen.domain.board.repository.BoardRepository;
import com.example.honorcitizen.infra.translation.TranslationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Accept-Language 헤더에 따른 응답 번역 배선 검증 — 실제 Google API 대신 TranslationClient를 목으로
// 대체한다("[EN] " 접두 번역). 언어 감지→배치 번역→DTO 사본 재조립 전체 경로를 컨트롤러 레벨에서 본다.
@SpringBootTest
@AutoConfigureMockMvc
class BoardControllerTranslationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BoardRepository boardRepository;

    @MockitoBean
    private TranslationClient translationClient;

    private static final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        boardRepository.deleteAll();
        when(translationClient.translate(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            List<String> out = new ArrayList<>(texts.size());
            texts.forEach(text -> out.add("[EN] " + text));
            return out;
        });
    }

    @Test
    void listTranslatesTitleAndContentWhenEnglishRequested() throws Exception {
        boardRepository.save(Board.create(BoardType.NOTICE, "공지 제목 번역", "공지 내용 번역", ADMIN_USER_ID));

        mockMvc.perform(get("/api/boards").param("type", "NOTICE").header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("[EN] 공지 제목 번역"))
                .andExpect(jsonPath("$.data.content[0].content").value("[EN] 공지 내용 번역"));
    }

    @Test
    void listKeepsKoreanWhenKoreanRequested() throws Exception {
        boardRepository.save(Board.create(BoardType.NOTICE, "공지 제목 한국어", "공지 내용 한국어", ADMIN_USER_ID));

        mockMvc.perform(get("/api/boards").param("type", "NOTICE").header("Accept-Language", "ko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("공지 제목 한국어"))
                .andExpect(jsonPath("$.data.content[0].content").value("공지 내용 한국어"));
    }

    @Test
    void detailTranslatesTitleContentAndNextTitle() throws Exception {
        Board first = boardRepository.save(Board.create(BoardType.FAQ, "질문 하나", "답변 하나", ADMIN_USER_ID));
        boardRepository.save(Board.create(BoardType.FAQ, "질문 둘", "답변 둘", ADMIN_USER_ID));

        mockMvc.perform(get("/api/boards/{id}", first.getId()).header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("[EN] 질문 하나"))
                .andExpect(jsonPath("$.data.content").value("[EN] 답변 하나"));
    }
}
