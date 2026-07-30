package com.example.honorcitizen.infra.toss;

public interface TossPaymentsClient {
    TossVirtualAccountResult issueVirtualAccount(TossVirtualAccountRequest request);
}
