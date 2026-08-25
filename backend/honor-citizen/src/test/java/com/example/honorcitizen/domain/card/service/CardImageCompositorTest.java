package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.exception.CustomException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 카드 이미지 합성 — 직인/발행처 필드는 정책상 제외(TODO.md 참고). 좌표계·팔레트 PNG 색상 버그
// 해결법은 명예한국인증/1을 실제 렌더링해 시안_최종.jpg와 육안 대조로 이미 검증했다(같은 로직).
// 여기서는 구조적 성질(유효 PNG·크기·입력별 예외)을 검증한다 — 픽셀 단위 정확성은 텍스트 렌더링
// 특성상 자동 검증이 어려워 육안 확인으로 별도 보완한다.
class CardImageCompositorTest {

    private final CardImageCompositor compositor = new CardImageCompositor();

    private CardMemberData sampleData() {
        return new CardMemberData("김학생", "Kim Hak-saeng", samplePhoto(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25));
    }

    private byte[] samplePhoto() {
        BufferedImage img = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 300, 400);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void composesHonorKoreanDesign1AsValidPng() throws Exception {
        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, sampleData());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(980);
        assertThat(result.getHeight()).isEqualTo(650);
    }

    @Test
    void composesHonorCitizenDesign1WithRealTitleImage() throws Exception {
        byte[] png = compositor.composeFront(CardTypeCode.HONOR_CITIZEN, 1, sampleData());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isGreaterThan(0);
        assertThat(result.getHeight()).isGreaterThan(0);
    }

    @Test
    void composesVisitorDesign2AsPortrait() throws Exception {
        byte[] png = compositor.composeFront(CardTypeCode.VISITOR, 2, sampleData());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result.getHeight()).isGreaterThan(result.getWidth()); // 세로형
    }

    @Test
    void composesHonorKoreanDesign6UsingNonStandardFileNames() throws Exception {
        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 6, sampleData());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result).isNotNull();
    }

    @Test
    void composesHonorCitizenDesign3UsingAliasedLogoFileName() throws Exception {
        // 명예한국인증/3은 발행처로고.png 대신 로고.png — 이번 범위는 발행처로고를 안 쓰므로 무관하지만
        // 다른 파일(앞면/사진)까지 정상 조회되는지 확인.
        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 3, sampleData());

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void handlesPhotoWithDifferentAspectRatioViaCoverFit() throws Exception {
        BufferedImage wide = new BufferedImage(800, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(wide, "png", out);
        CardMemberData data = new CardMemberData("김학생", "Kim Hak-saeng", out.toByteArray(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25));

        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, data);

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void throwsForCardTypeWithoutLayout() {
        assertThatThrownBy(() -> compositor.composeFront(CardTypeCode.STUDENT, 1, sampleData()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void throwsForVisitorDesign1BecauseFrontBackRoleIsUnverified() {
        // 방문증/1은 앞면.png/뒷면.png 구분이 없는 "대지 1.png" 하나뿐이라 앞뒤 역할이 확인 전이다.
        assertThatThrownBy(() -> compositor.composeFront(CardTypeCode.VISITOR, 1, sampleData()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void nullPhotoSkipsPhotoDrawingWithoutError() throws Exception {
        CardMemberData data = new CardMemberData("김학생", "Kim Hak-saeng", null,
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25));

        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, data);

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }
}
