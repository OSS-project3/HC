package com.example.honorcitizen.infra.toss;

import java.time.LocalDateTime;

public record TossVirtualAccountResult(
        String paymentKey,
        String bankName,
        String accountNumber,
        LocalDateTime dueDate
) {}
