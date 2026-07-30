package com.example.honorcitizen.infra.toss;

public record TossVirtualAccountRequest(
        long amount,
        String orderId,
        String orderName,
        String customerName,
        String bank,
        int validHours
) {}
