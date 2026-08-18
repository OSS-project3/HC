package com.example.honorcitizen.infra.mail;

import com.example.honorcitizen.common.enums.EmailType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class SmtpEmailSenderTest {

    @Autowired
    private EmailSender emailSender;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void sendsMimeMessageWithExpectedHeaders() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);

        emailSender.send("user@example.com", EmailType.SIGNUP_VERIFICATION, "인증 코드 안내", "<p>html</p>", "text");

        verify(javaMailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("인증 코드 안내");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
    }

    @Test
    void throwsEmailDeliveryFailedWhenSmtpSendFails() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("smtp down")).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() ->
                emailSender.send("user@example.com", EmailType.SIGNUP_VERIFICATION, "subject", "<p>html</p>", "text"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DELIVERY_FAILED);
    }
}
