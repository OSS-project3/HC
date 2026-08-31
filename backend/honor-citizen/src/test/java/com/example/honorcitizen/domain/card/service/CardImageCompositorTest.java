package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.SchoolType;
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
        return new CardMemberData("김", "학생", "Kim Hak-saeng", null, null, null, samplePhoto(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), null, null, null);
    }

    private CardMemberData sampleDataWithHanja() {
        return new CardMemberData("김", "학생", "Kim Hak-saeng", "學生",
                "배울 학(學) 날 생(生)", "배우고 익히며 성장하는 삶을 산다.", samplePhoto(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), "인", null, null);
    }

    private byte[] samplePhoto() {
        return solidPng(300, 400, Color.LIGHT_GRAY);
    }

    // 4-C: 학생증은 배경(템플릿)이 classpath가 아니라 S3에서 온 바이트라(4-D 업로드 API 결과),
    // 여기서는 실제 학교 이미지 대신 단색 캔버스로 구조(유효 PNG·크기·필드 배치)만 검증한다 — 다른
    // 3종과 동일하게 픽셀 단위 정확성은 육안 확인으로 별도 보완한다(실제 렌더링은 4-C 작업 중 확인).
    // 980×650/650×980은 기존 3종 실제 템플릿 해상도(기준 캔버스의 ~4.17배)와 동일한 비율.
    private CardMemberData studentData(SchoolType schoolType, CardDesignOrientation orientation, boolean hanja) {
        boolean landscape = orientation == CardDesignOrientation.LANDSCAPE;
        byte[] template = solidPng(landscape ? 980 : 650, landscape ? 650 : 980, Color.WHITE);
        return new CardMemberData(
                "김", "학생", "Kim Hak-saeng", hanja ? "學生" : null,
                hanja ? "배울 학(學) 날 생(生)" : null, hanja ? "배우고 익히며 성장하는 삶을 산다." : null,
                samplePhoto(), "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), "인",
                null, null, schoolType, orientation,
                schoolType == SchoolType.UNIVERSITY ? "202500225" : null,
                schoolType == SchoolType.UNIVERSITY ? "인문대학 사회복지학과" : null,
                schoolType == SchoolType.HIGH_SCHOOL ? LocalDate.of(2009, 12, 8) : null,
                template, template);
    }

    private byte[] solidPng(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
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
        CardMemberData data = new CardMemberData("김", "학생", "Kim Hak-saeng", null, null, null, out.toByteArray(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), null, null, null);

        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, data);

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void throwsForStudentFrontWithoutStudentSpecificData() {
        // 4-C: STUDENT는 이제 실제 레이아웃(CardLayouts.STUDENT_FRONT)이 있다 — 이 테스트가 예외를
        // 검증하는 이유는 "레이아웃 없음"이 아니라 sampleData()(학생증 전용 필드 없는 구식 13-인자
        // 생성자)가 studentOrientation=null/templateFront=null로 만들어져서다. 실제 STUDENT 성공
        // 경로는 아래 composesStudent*() 테스트들이 studentData()로 검증한다.
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
        CardMemberData data = new CardMemberData("김", "학생", "Kim Hak-saeng", null, null, null, null,
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), null, null, null);

        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, data);

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void drawsZodiacIconWhenBranchProvided() throws Exception {
        CardMemberData data = sampleDataWithHanja();

        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, data);

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void throwsWhenZodiacBranchIsInvalid() {
        CardMemberData data = new CardMemberData("김", "학생", "Kim Hak-saeng", null, null, null, samplePhoto(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), "존재안함", null, null);

        assertThatThrownBy(() -> compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, data))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void drawsLogoAndSealWhenBytesProvided() throws Exception {
        BufferedImage logoImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(logoImg, "png", out);
        CardMemberData data = new CardMemberData("김", "학생", "Kim Hak-saeng", null, null, null, samplePhoto(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), null,
                out.toByteArray(), out.toByteArray());

        byte[] png = compositor.composeFront(CardTypeCode.HONOR_KOREAN, 1, data);

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void drawsSealForHonorCitizenDesign2AfterAssetAdded() throws Exception {
        // HONOR_CITIZEN/2엔 원래 직인.png가 없었다(에셋 누락) — HONOR_CITIZEN/1 것을 그대로 복사해
        // 채워넣었다(슬롯 크기 참고용일 뿐 실제 콘텐츠로는 안 쓰여 재사용 가능, 2026-08-26 해소).
        // 슬롯 파일이 아예 없는 디자인은 drawSlotImage()가 조용히 건너뛰도록 여전히 방어돼 있다.
        BufferedImage sealImg = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(sealImg, "png", out);
        CardMemberData data = new CardMemberData("김", "학생", "Kim Hak-saeng", null, null, null, samplePhoto(),
                "ROK-12345-6789", "대한민국 전라북도 전주시", LocalDate.of(2026, 8, 25), null, null, out.toByteArray());

        byte[] png = compositor.composeFront(CardTypeCode.HONOR_CITIZEN, 2, data);

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void composesHonorKoreanBackWithHanjaVariant() throws Exception {
        byte[] png = compositor.composeBack(CardTypeCode.HONOR_KOREAN, 1, sampleDataWithHanja());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result).isNotNull();
        // 뒷면.png는 앞면.png와 별도로 export된 에셋이라 픽셀 치수가 정확히 같다는 보장은 없다(HONOR_KOREAN/1
        // 실측: 980x651 vs 앞면 980x650) — 세로형/가로형 여부만 구조적으로 검증한다.
        assertThat(result.getWidth()).isGreaterThan(result.getHeight());
    }

    @Test
    void composesHonorKoreanBackWithoutHanjaVariant() throws Exception {
        byte[] png = compositor.composeBack(CardTypeCode.HONOR_KOREAN, 1, sampleData());

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void composesHonorKoreanDesign6BackUsingNonStandardFileName() throws Exception {
        // 명예한국인증/6은 뒷면.png 대신 "대지 1 사본.png".
        byte[] png = compositor.composeBack(CardTypeCode.HONOR_KOREAN, 6, sampleDataWithHanja());

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void throwsForVisitorDesign1BackBecauseFrontBackRoleIsUnverified() {
        assertThatThrownBy(() -> compositor.composeBack(CardTypeCode.VISITOR, 1, sampleDataWithHanja()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void throwsBackForStudentWithoutStudentSpecificData() {
        // 위 throwsForStudentFrontWithoutStudentSpecificData와 동일한 이유(구식 sampleDataWithHanja()엔
        // studentOrientation/templateBack이 없음) — STUDENT_BACK 레이아웃 자체는 이제 존재한다.
        assertThatThrownBy(() -> compositor.composeBack(CardTypeCode.STUDENT, 1, sampleDataWithHanja()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void composesStudentUniversityLandscapeFrontAsValidPng() throws Exception {
        byte[] png = compositor.composeFront(CardTypeCode.STUDENT, 1,
                studentData(SchoolType.UNIVERSITY, CardDesignOrientation.LANDSCAPE, false));

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(980);
        assertThat(result.getHeight()).isEqualTo(650);
    }

    @Test
    void composesStudentHighSchoolPortraitFrontAsValidPng() throws Exception {
        byte[] png = compositor.composeFront(CardTypeCode.STUDENT, 1,
                studentData(SchoolType.HIGH_SCHOOL, CardDesignOrientation.PORTRAIT, false));

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(650);
        assertThat(result.getHeight()).isEqualTo(980);
    }

    @Test
    void composesStudentUniversityPortraitFrontAsValidPng() throws Exception {
        // 세로형 학번 칸은 원문 좌표를 그대로 쓴다(생년월일만 보정 대상) — 대학교 케이스로 확인.
        byte[] png = compositor.composeFront(CardTypeCode.STUDENT, 1,
                studentData(SchoolType.UNIVERSITY, CardDesignOrientation.PORTRAIT, false));

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void composesStudentHighSchoolLandscapeFrontAsValidPng() throws Exception {
        byte[] png = compositor.composeFront(CardTypeCode.STUDENT, 1,
                studentData(SchoolType.HIGH_SCHOOL, CardDesignOrientation.LANDSCAPE, false));

        assertThat(ImageIO.read(new ByteArrayInputStream(png))).isNotNull();
    }

    @Test
    void composesStudentBackWithHanjaVariant() throws Exception {
        byte[] png = compositor.composeBack(CardTypeCode.STUDENT, 1,
                studentData(SchoolType.UNIVERSITY, CardDesignOrientation.LANDSCAPE, true));

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(980);
        assertThat(result.getHeight()).isEqualTo(650);
    }

    @Test
    void composesStudentBackWithoutHanjaVariant() throws Exception {
        byte[] png = compositor.composeBack(CardTypeCode.STUDENT, 1,
                studentData(SchoolType.HIGH_SCHOOL, CardDesignOrientation.PORTRAIT, false));

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(650);
        assertThat(result.getHeight()).isEqualTo(980);
    }

    @Test
    void throwsForStudentFrontWhenTemplateMissing() {
        CardMemberData data = studentData(SchoolType.UNIVERSITY, CardDesignOrientation.LANDSCAPE, false);
        CardMemberData withoutTemplate = new CardMemberData(
                data.surname(), data.name(), data.englishName(), data.chineseName(), data.nameMeaning(),
                data.nameInterpretation(), data.photo(), data.cardNumber(), data.address(), data.issueDate(),
                data.zodiacBranch(), data.logo(), data.seal(), data.schoolType(), data.studentOrientation(),
                data.studentId(), data.department(), data.birthDate(), null, null);

        assertThatThrownBy(() -> compositor.composeFront(CardTypeCode.STUDENT, 1, withoutTemplate))
                .isInstanceOf(CustomException.class);
    }
}
