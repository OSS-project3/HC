package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.board.entity.Board;
import com.example.honorcitizen.domain.board.repository.BoardAttachmentRepository;
import com.example.honorcitizen.domain.board.repository.BoardRepository;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoardAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private BoardAttachmentRepository boardAttachmentRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;

    @MockitoBean
    private StorageService storageService;

    private String adminToken;
    private String userToken;

    private static final String CREATE_REQUEST_JSON = """
            {
              "boardType": "NOTICE",
              "title": "제목",
              "content": "내용"
            }
            """;

    private static final String UPDATE_REQUEST_JSON = """
            {
              "boardType": "FAQ",
              "title": "수정된 제목",
              "content": "수정된 내용"
            }
            """;

    @BeforeEach
    void setUp() {
        boardAttachmentRepository.deleteAll();
        uploadFileRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.createNewUser("board-admin@example.com", "oauth-board-admin", "google", "관리자"));
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin.getId(), UserRole.ADMIN);

        User user = userRepository.save(User.createNewUser("board-user@example.com", "oauth-board-user", "google", "일반사용자"));
        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), UserRole.USER);

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    @Test
    void createReturnsCreatedForAdmin() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", CREATE_REQUEST_JSON.getBytes());

        mockMvc.perform(multipart("/api/admin/boards")
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

        mockMvc.perform(multipart("/api/admin/boards")
                        .file(requestPart)
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReturnsUnauthorizedWithoutToken() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", CREATE_REQUEST_JSON.getBytes());

        mockMvc.perform(multipart("/api/admin/boards")
                        .file(requestPart))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateSucceedsForAdmin() throws Exception {
        Board board = boardRepository.save(Board.create(BoardType.NOTICE, "원래 제목", "원래 내용", 1L));

        mockMvc.perform(patch("/api/admin/boards/{id}", board.getId())
                        .contentType("application/json")
                        .content(UPDATE_REQUEST_JSON)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateReturnsForbiddenForNonAdmin() throws Exception {
        Board board = boardRepository.save(Board.create(BoardType.NOTICE, "원래 제목", "원래 내용", 1L));

        mockMvc.perform(patch("/api/admin/boards/{id}", board.getId())
                        .contentType("application/json")
                        .content(UPDATE_REQUEST_JSON)
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSucceedsForAdmin() throws Exception {
        Board board = boardRepository.save(Board.create(BoardType.NOTICE, "원래 제목", "원래 내용", 1L));

        mockMvc.perform(delete("/api/admin/boards/{id}", board.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteReturnsForbiddenForNonAdmin() throws Exception {
        Board board = boardRepository.save(Board.create(BoardType.NOTICE, "원래 제목", "원래 내용", 1L));

        mockMvc.perform(delete("/api/admin/boards/{id}", board.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReturnsUnauthorizedWithoutToken() throws Exception {
        Board board = boardRepository.save(Board.create(BoardType.NOTICE, "원래 제목", "원래 내용", 1L));

        mockMvc.perform(delete("/api/admin/boards/{id}", board.getId()))
                .andExpect(status().isUnauthorized());
    }
}
