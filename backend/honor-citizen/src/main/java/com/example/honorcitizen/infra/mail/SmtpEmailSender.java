package com.example.honorcitizen.infra.mail;

import com.example.honorcitizen.common.enums.EmailType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender javaMailSender, @Value("${app.mail.from}") String from) {
        this.javaMailSender = javaMailSender;
        this.from = from;
    }

    @Override
    public void send(String to, EmailType emailType, String subject, String htmlBody, String textBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // multipart=true 필수 — setText(text, html) 2-인자 오버로드(멀티파트 alternative)는
            // 헬퍼가 멀티파트 모드가 아니면 IllegalStateException을 던진다.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            // text를 먼저 세팅하고 html을 추가해야 multipart/alternative(text 우선, html 폴백)로 구성된다.
            helper.setText(textBody, htmlBody);

            javaMailSender.send(message);
            log.info("이메일 발송 성공 type={} recipient={}", emailType, maskEmail(to));
        } catch (MessagingException | MailException e) {
            // 이메일 전문·인증 코드·수신자 전체 주소는 로그에 남기지 않는다(정책).
            log.warn("이메일 발송 실패 type={} recipient={}", emailType, maskEmail(to));
            throw new CustomException(ErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }

    // 로그에 전체 이메일 주소를 남기지 않기 위한 최소 마스킹 — 앞 1글자 + 도메인만 남긴다.
    private String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
