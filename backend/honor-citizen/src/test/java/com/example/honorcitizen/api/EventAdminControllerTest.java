package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.event.entity.EventPost;
import com.example.honorcitizen.domain.event.repository.EventImageRepository;
import com.example.honorcitizen.domain.event.repository.EventPostRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private EventPostRepository eventPostRepository;
    @Autowired
    private EventImageRepository eventImageRepository;

    @MockitoBean
    private StorageService storageService;

    private String adminToken;
    private String userToken;

    private static final String CREATE_REQUEST_JSON = """
            {
              "eventType": "BOOTH",
              "title": "서울공예트렌드페어",
              "eventDateText": "2026. 12",
              "place": "서울 코엑스 Hall C",
              "host": "한국공예·디자인문화진흥원",
              "cardLabel": "명예한국인증 · 방문증",
              "content": "부스를 찾은 방문객에게..."
            }
            """;

    private static final String UPDATE_REQUEST_JSON = """
            {
              "eventType": "COLLABORATION",
              "title": "수정된 제목",
              "eventDateText": "2026. 05",
              "place": "수정된 장소",
              "host": "수정된 주최",
              "cardLabel": "수정된 카드",
              "content": "수정된 내용",
              "visible": false,
              "displayOrder": 1
            }
            """;

    @BeforeEach
    void setUp() {
        eventImageRepository.deleteAll();
        eventPostRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.createNewUser("event-admin@example.com", "oauth-event-admin", "google", "관리자"));
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin.getId(), UserRole.ADMIN);

        User user = userRepository.save(User.createNewUser("event-user@example.com", "oauth-event-user", "google", "일반사용자"));
        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), UserRole.USER);

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    @Test
    void createReturnsCreatedForAdmin() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", CREATE_REQUEST_JSON.getBytes());

        mockMvc.perform(multipart("/api/admin/events")
                        .file(requestPart)
                        .header("Authorization", adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createReturnsForbiddenForNonAdmin() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", CREATE_REQUEST_JSON.getBytes());

        mockMvc.perform(multipart("/api/admin/events")
                        .file(requestPart)
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReturnsUnauthorizedWithoutToken() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", CREATE_REQUEST_JSON.getBytes());

        mockMvc.perform(multipart("/api/admin/events")
                        .file(requestPart))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateSucceedsForAdmin() throws Exception {
        EventPost eventPost = eventPostRepository.save(EventPost.create(EventType.BOOTH, "원래 제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, true, null));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", UPDATE_REQUEST_JSON.getBytes());

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, "/api/admin/events/{id}", eventPost.getId())
                        .file(requestPart)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateReturnsForbiddenForNonAdmin() throws Exception {
        EventPost eventPost = eventPostRepository.save(EventPost.create(EventType.BOOTH, "원래 제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, true, null));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", UPDATE_REQUEST_JSON.getBytes());

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, "/api/admin/events/{id}", eventPost.getId())
                        .file(requestPart)
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSucceedsForAdmin() throws Exception {
        EventPost eventPost = eventPostRepository.save(EventPost.create(EventType.BOOTH, "원래 제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, true, null));

        mockMvc.perform(delete("/api/admin/events/{id}", eventPost.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteReturnsForbiddenForNonAdmin() throws Exception {
        EventPost eventPost = eventPostRepository.save(EventPost.create(EventType.BOOTH, "원래 제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, true, null));

        mockMvc.perform(delete("/api/admin/events/{id}", eventPost.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReturnsUnauthorizedWithoutToken() throws Exception {
        EventPost eventPost = eventPostRepository.save(EventPost.create(EventType.BOOTH, "원래 제목", null, "2026. 01",
                "장소", "주최", "카드", "내용", null, true, null));

        mockMvc.perform(delete("/api/admin/events/{id}", eventPost.getId()))
                .andExpect(status().isUnauthorized());
    }
}
