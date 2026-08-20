package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.domain.event.entity.EventPost;
import com.example.honorcitizen.domain.event.repository.EventImageRepository;
import com.example.honorcitizen.domain.event.repository.EventPostRepository;
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
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventPostRepository eventPostRepository;
    @Autowired
    private EventImageRepository eventImageRepository;

    @BeforeEach
    void setUp() {
        eventImageRepository.deleteAll();
        eventPostRepository.deleteAll();
    }

    @Test
    void listSucceedsWithoutAuthentication() throws Exception {
        eventPostRepository.save(EventPost.create(EventType.BOOTH, "서울공예트렌드페어", null, "2026. 12",
                "서울 코엑스 Hall C", "한국공예·디자인문화진흥원", "명예한국인증 · 방문증", "내용", null, null, null, true, null));

        mockMvc.perform(get("/api/events").param("type", "BOOTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("서울공예트렌드페어"));
    }

    @Test
    void listHidesInvisiblePosts() throws Exception {
        eventPostRepository.save(EventPost.create(EventType.BOOTH, "비공개", null, "2026. 12",
                "장소", "주최", "카드", "내용", null, null, null, false, null));

        mockMvc.perform(get("/api/events").param("type", "BOOTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void listReturnsBadRequestWhenTypeMissing() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void detailSucceedsWithoutAuthentication() throws Exception {
        EventPost eventPost = eventPostRepository.save(EventPost.create(EventType.COLLABORATION, "협업", null, "2026. 12",
                "장소", "주최", "카드", "내용", null, null, null, true, null));

        mockMvc.perform(get("/api/events/{id}", eventPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("협업"))
                .andExpect(jsonPath("$.data.images").isArray())
                .andExpect(jsonPath("$.data.images").isEmpty());
    }

    @Test
    void detailReturnsNotFoundForHiddenPost() throws Exception {
        EventPost hidden = eventPostRepository.save(EventPost.create(EventType.BOOTH, "비공개", null, "2026. 12",
                "장소", "주최", "카드", "내용", null, null, null, false, null));

        mockMvc.perform(get("/api/events/{id}", hidden.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EVENT_NOT_FOUND"));
    }

    @Test
    void detailReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/events/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EVENT_NOT_FOUND"));
    }
}
