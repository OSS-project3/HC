package com.example.honorcitizen.infra.toss;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.toss")
public record TossPaymentsProperties(
        String baseUrl,
        String secretKey,
        String webhookSecret,
        String defaultBank
) {}
