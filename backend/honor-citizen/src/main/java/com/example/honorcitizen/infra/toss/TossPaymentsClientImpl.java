package com.example.honorcitizen.infra.toss;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Component
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class TossPaymentsClientImpl implements TossPaymentsClient {

    private final RestClient restClient;

    public TossPaymentsClientImpl(TossPaymentsProperties props) {
        String encoded = Base64.getEncoder()
                .encodeToString((props.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public TossVirtualAccountResult issueVirtualAccount(TossVirtualAccountRequest request) {
        try {
            TossVirtualAccountResponse response = restClient.post()
                    .uri("/v1/virtual-accounts")
                    .body(request)
                    .retrieve()
                    .body(TossVirtualAccountResponse.class);

            if (response == null || response.getVirtualAccount() == null) {
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }

            LocalDateTime dueDate = parseDueDate(response.getVirtualAccount().getDueDate());
            String bankName = response.getVirtualAccount().getBank() != null
                    ? response.getVirtualAccount().getBank() + "은행"
                    : response.getVirtualAccount().getBankCode();

            return new TossVirtualAccountResult(
                    response.getPaymentKey(),
                    bankName,
                    response.getVirtualAccount().getAccountNumber(),
                    dueDate
            );
        } catch (CustomException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("토스페이먼츠 가상계좌 발급 실패", e);
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
    }

    private LocalDateTime parseDueDate(String dueDate) {
        if (dueDate == null) {
            return LocalDateTime.now().plusHours(24);
        }
        try {
            return OffsetDateTime.parse(dueDate).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dueDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ex) {
                return LocalDateTime.now().plusHours(24);
            }
        }
    }

    // --- 토스페이먼츠 응답 매핑 ---

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TossVirtualAccountResponse {
        private String paymentKey;
        private VirtualAccountInfo virtualAccount;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class VirtualAccountInfo {
        private String accountNumber;
        private String bankCode;
        private String bank;
        private String customerName;
        private String dueDate;
    }
}
