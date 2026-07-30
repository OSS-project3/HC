package com.example.honorcitizen.domain.payment.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class VirtualAccountResponse {

    private final String bankName;
    private final String accountNumber;
    private final long amount;
    private final String depositorName;
    private final LocalDateTime expiredAt;

    private VirtualAccountResponse(String bankName, String accountNumber, long amount,
                                   String depositorName, LocalDateTime expiredAt) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.depositorName = depositorName;
        this.expiredAt = expiredAt;
    }

    public static VirtualAccountResponse of(String bankName, String accountNumber, long amount,
                                            String depositorName, LocalDateTime expiredAt) {
        return new VirtualAccountResponse(bankName, accountNumber, amount, depositorName, expiredAt);
    }
}
