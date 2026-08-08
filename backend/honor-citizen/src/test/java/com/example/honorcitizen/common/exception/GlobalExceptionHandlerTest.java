package com.example.honorcitizen.common.exception;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GlobalExceptionHandler.handleValidationException(MethodArgumentNotValidException)의 다중 필드 오류
// 응답 조립 로직만 검증한다. @NotBlank/@ValidNationality 등 개별 애노테이션 자체의 동작은
// 이미 Hibernate Validator/커스텀 Validator 단위 테스트가 보장하므로 다시 검증하지 않는다.
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StorageService storageService;

    private String token;
    private CardType cardType;

    @BeforeEach
    void setUp() {
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.createNewUser("global-ex@example.com", "oauth-global-ex", "google", "Tester");
        user.agreeTerms(true, true, true);
        user = userRepository.save(user);
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-gex", null, BigDecimal.valueOf(30000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    @Test
    void validationFailureReturnsAllFieldErrorsWithNestedPropertyPaths() throws Exception {
        // applicant.phone(@NotBlank)과 member.nationality(@ValidNationality)를 동시에 위반시킨다.
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "applicant": { "name": "홍길동", "phone": "" },
                  "member": { "englishName": "Hong Gildong", "birthDate": "1990-05-15", "nationality": "USA", "gender": "MALE" }
                }
                """.formatted(cardType.getId());

        JsonNode body = performCreateAndParseBody(json);

        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("errorCode").asText()).isEqualTo("INVALID_INPUT");
        JsonNode errors = body.get("errors");
        assertThat(errors).hasSize(2);
        // errors[]만 field 기준으로 정렬(결정론적 순서): "applicant.phone" < "member.nationality"
        assertThat(errors.get(0).get("field").asText()).isEqualTo("applicant.phone");
        assertThat(errors.get(1).get("field").asText()).isEqualTo("member.nationality");
        // 최상위 errorMessage는 (하위 호환을 위해) 기존과 동일하게 BindingResult의 원래 첫 오류 메시지를
        // 그대로 쓴다 — errors[] 정렬과 무관하므로, 정렬된 두 항목 중 하나와 일치하기만 하면 된다.
        assertThat(body.get("errorMessage").asText())
                .isIn(errors.get(0).get("message").asText(), errors.get(1).get("message").asText());
    }

    @Test
    void singleFieldValidationFailureStaysCompatibleWithPreviousSingleMessageContract() throws Exception {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "applicant": { "name": "홍길동", "phone": "" },
                  "member": { "englishName": "Hong Gildong", "birthDate": "1990-05-15", "nationality": "US", "gender": "MALE" }
                }
                """.formatted(cardType.getId());

        JsonNode body = performCreateAndParseBody(json);

        assertThat(body.get("errorCode").asText()).isEqualTo("INVALID_INPUT");
        assertThat(body.get("errorMessage").asText()).isNotBlank();
        JsonNode errors = body.get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).get("field").asText()).isEqualTo("applicant.phone");
        assertThat(body.get("errorMessage").asText()).isEqualTo(errors.get(0).get("message").asText());
    }

    private JsonNode performCreateAndParseBody(String requestJson) throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", requestJson.getBytes());
        MockMultipartFile photoPart = new MockMultipartFile("photo", "face.jpg", "image/jpeg", imageBytes());

        MvcResult result = mockMvc.perform(multipart("/api/applications")
                        .file(requestPart)
                        .file(photoPart)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private byte[] imageBytes() {
        try {
            BufferedImage image = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
