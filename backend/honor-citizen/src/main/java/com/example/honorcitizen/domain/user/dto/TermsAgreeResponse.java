package com.example.honorcitizen.domain.user.dto;

import com.example.honorcitizen.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TermsAgreeResponse {

    private final boolean termsAgreed;
    private final boolean privacyAgreed;
    private final boolean imageUploadAgreed;
    private final boolean shippingAgreed;
    private final LocalDateTime agreedAt;

    private TermsAgreeResponse(User user) {
        this.termsAgreed = user.isTermsAgreed();
        this.privacyAgreed = user.isPrivacyAgreed();
        this.imageUploadAgreed = user.isImageUploadAgreed();
        this.shippingAgreed = user.isShippingAgreed();
        this.agreedAt = user.getTermsAgreedAt();
    }

    public static TermsAgreeResponse from(User user) {
        return new TermsAgreeResponse(user);
    }
}
