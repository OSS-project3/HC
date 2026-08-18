package com.example.honorcitizen.infra.mail;

import com.example.honorcitizen.common.enums.EmailType;

// 도메인 계층은 JavaMailSender를 직접 쓰지 않고 이 인터페이스만 거친다(SMTP 공급자 교체·테스트 목킹 용이).
// send()가 정상 반환하면 SMTP 서버가 메일을 접수했다는 뜻이지 수신함 도착을 보장하지 않는다.
// 실패 시 CustomException(ErrorCode.EMAIL_DELIVERY_FAILED)을 던진다. 자동 재시도는 하지 않는다(호출자 책임).
public interface EmailSender {

    void send(String to, EmailType emailType, String subject, String htmlBody, String textBody);
}
