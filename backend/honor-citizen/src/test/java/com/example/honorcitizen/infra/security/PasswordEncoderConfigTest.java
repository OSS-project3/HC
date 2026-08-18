package com.example.honorcitizen.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PasswordEncoderConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void encodesWithBcryptPrefix() {
        String encoded = passwordEncoder.encode("plain-password");

        assertThat(encoded).startsWith("{bcrypt}");
    }

    @Test
    void matchesEncodedValueAgainstOriginalPlainText() {
        String encoded = passwordEncoder.encode("plain-password");

        assertThat(passwordEncoder.matches("plain-password", encoded)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", encoded)).isFalse();
    }
}
