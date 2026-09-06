package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedString;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;

/**
 * 신청서 확정 정보를 카드 템플릿(디자이너 제공 시안, `resources/card-templates/`)에 좌표 기반으로
 * 합성해 카드 앞면 이미지를 만든다(DESIGN.md/card.md가 "미구현"이라고 적어뒀던 CardFieldDefinition).
 *
 * 직인·발행처 필드는 실제 지자체 관인을 무단으로 인쇄하게 되는 문제가 있어 이번 범위에서 제외했다
 * (TODO.md "카드 이미지 합성" 섹션 정책 참고).
 *
 * [좌표계] {@link CardLayouts}의 오프셋은 위치값.jpg 표를 그대로 옮긴 값 — 기준 캔버스(카드종류별
 * baseWidth×baseHeight) 중심(0,0)으로부터의 거리다. 실제 템플릿 PNG는 해상도가 더 커서
 * (실제크기/기준크기) 배율을 곱해 변환한다. 이 해석은 명예한국인증 디자인 1을 실제 렌더링해
 * `시안_최종.jpg`와 육안 대조로 검증했다(TODO.md 참고, 분석적으로 유도한 값이 아니다).
 *
 * [팔레트 PNG 색상 버그] 템플릿 다수가 인덱스 컬러 PNG인데 {@code ImageIO.read()}로 읽으면
 * 색이 깨진다(빨강/파랑 반전 도트 패턴). {@link Toolkit} 경로로 우회해서 읽어야 한다 — 이 클래스의
 * 모든 이미지 로딩은 반드시 {@link #loadImage}를 거친다.
 */
@Component
class CardImageCompositor {

    private static final String TEMPLATE_ROOT = "card-templates/";
    private static final DateTimeFormatter ISSUE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    // 앞·뒷면 배경/사진/로고/직인 자리 파일명이 디자인마다 다르게 export되어 있어(디자이너 파일명
    // 비표준) 후보를 순서대로 시도한다. TODO.md "카드 이미지 합성"/2-B 섹션의 파일명 불일치 조사 결과.
    private static final List<String> FRONT_CANDIDATES = List.of("앞면.png", "대지 1.png");
    private static final List<String> BACK_CANDIDATES = List.of("뒷면.png", "대지 1 사본.png");
    private static final List<String> PHOTO_CANDIDATES = List.of("사진.png", "아트보드 2.png");
    private static final List<String> LOGO_CANDIDATES = List.of("발행처로고.png", "로고.png", "발행처 로고.png");
    private static final List<String> SEAL_CANDIDATES = List.of("직인.png", "아트보드 6.png");
    // 띠 아이콘은 디자인별 슬롯 에셋이 없어(HONOR_CITIZEN/1의 캐릭터.png가 유일한 참고용) 슬롯 크기에
    // coverFit하지 않고, 기준 캔버스 스케일로 이 논리 너비(pt)만큼 그린다. 참고 이미지(185x249)가
    // 그 카드 배경(980x650) 대비 차지하는 실제 비율(185/980≈18.9%)로 환산한 값 — 기존 9pt는 이 비율의
    // 1/5 수준으로 눈에 띄게 작게 그려지는 버그였다(사용자 리포트: "용 이미지가 너무 작게 출력되는데").
    private static final double ZODIAC_BASE_WIDTH = 40d;

    // 뒷면 뜻풀이(nameInterpretation) 줄바꿈 폭 — baseWidth 대비 비율. 실제 700개 추천 이름 데이터셋의
    // meaning 필드(평균 약 70자, 최장 97자)를 이 비율로 실측 줄바꿈한 결과 전부 2~3줄로 떨어짐을
    // 확인했다(2026-09-05, KoPub Dotum Medium 실폰트로 직접 측정 — WrapTest 스크립트, 커밋 미포함).
    // 카드 뒷면 그래픽(좌우 도자기 문양)과 겹치지 않는 중앙 여백에 대응하는 값이다.
    private static final double INTERPRETATION_WIDTH_RATIO = 0.55;
    // 학생증 뒷면은 풀이 폰트가 더 커서(8f vs 4f) 같은 비율이면 줄 수가 더 늘어난다 — 학생증 배경은
    // 좌우 여백이 더 넓어(점무늬·물결무늬가 훨씬 가장자리에 있음, 실제 렌더링으로 확인) 폭을
    // 넓혀도 그래픽과 안 겹친다. 이 값으로 같은 최장(97자) 텍스트가 3~4줄로 카드 안에 들어간다.
    private static final double STUDENT_INTERPRETATION_WIDTH_RATIO = 0.72;

    private final Font dotumMedium;
    private final Font dotumBold;
    private final Font batangBold;
    // 4-E: CJK fallback — KoPub 3종은 KS X 1001 위주라 이름에 드물게 쓰이는 한자(예: 昀, 妸)가
    // 빠져 있다(실측 확인, 2026-09-01). 이 폰트는 못 바꾸고(4종 카드 전체가 이미 이 룩으로
    // 시안_최종.jpg 대조 검증돼 있어 전면 교체 시 재검증 범위가 너무 커짐), KoPub이 못 그리는
    // 글자만 이 폰트로 대신 그린다 — Noto Sans KR(SIL OFL, notofonts/noto-cjk 정적 OTF), 한국어
    // 문맥 기준 서브셋이라 완전한 유니코드 한자 커버리지는 아니지만(예: 娍, CJK 확장A 일부는
    // 이것도 못 그림) KoPub 단독보다 실측으로 커버리지가 넓다.
    private final Font cjkFallback;
    private final ConcurrentHashMap<String, BufferedImage> imageCache = new ConcurrentHashMap<>();

    CardImageCompositor() {
        this.dotumMedium = loadFont("KoPub Dotum_Pro Medium.otf");
        this.dotumBold = loadFont("KoPub Dotum_Pro Bold.otf");
        this.batangBold = loadFont("KoPub Batang_Pro Bold.otf");
        this.cjkFallback = loadFont("NotoSansKR-Regular.otf");
    }

    byte[] composeFront(CardTypeCode cardType, int design, CardMemberData data) {
        if (cardType == CardTypeCode.STUDENT) {
            return composeStudentFront(data);
        }
        CardLayout layout = CardLayouts.FRONT.get(cardType);
        if (layout == null || isUnverifiedDesign(cardType, design)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String dir = TEMPLATE_ROOT + cardType.name() + "/" + design + "/";
        BufferedImage bg = copy(resolveImage(dir, FRONT_CANDIDATES));
        double scaleX = bg.getWidth() / layout.baseWidth();
        double scaleY = bg.getHeight() / layout.baseHeight();

        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        try {
            drawTitle(g, cardType, layout, scaleX, scaleY);
            drawPhoto(g, dir, data.photo(), layout, scaleX, scaleY);
            drawText(g, data.fullName(), dotumBold, 8f, Color.BLACK, layout.name(), layout, scaleX, scaleY);
            // HONOR_CITIZEN/HONOR_KOREAN 위치값 표는 영문명/카드번호/주소를 각자 다른 x에서 중앙
            // 정렬하는 걸 전제로 한 듯한데, 문자열 길이가 서로 달라(특히 주소) 실제 렌더링해보면
            // 왼쪽 시작점이 들쭉날쭉했다(사용자 확인, 두 카드종류 모두 동일 원칙 적용 요청). 이름의
            // 왼쪽 끝을 기준선으로 계산해 그 지점에 왼쪽 정렬한다. 각 필드 자신의 y좌표·폰트 크기는
            // 그대로 쓴다. VISITOR는 요청 범위 밖이라 기존 중앙 정렬 유지.
            if (cardType == CardTypeCode.HONOR_CITIZEN || cardType == CardTypeCode.HONOR_KOREAN) {
                double nameLeftEdge = leftEdgeX(data.fullName(), dotumBold, 8f, layout.name(), layout, scaleX);
                drawTextAtPixelX(g, data.englishName(), dotumMedium, 5f, Color.DARK_GRAY, nameLeftEdge,
                        layout.englishName(), layout, scaleX, scaleY);
                drawTextAtPixelX(g, data.cardNumber(), dotumMedium, 5f, Color.DARK_GRAY, nameLeftEdge,
                        layout.cardNumber(), layout, scaleX, scaleY);
                drawTextAtPixelX(g, data.address(), dotumMedium, 4.5f, Color.DARK_GRAY, nameLeftEdge,
                        layout.address(), layout, scaleX, scaleY);
            } else {
                drawText(g, data.englishName(), dotumMedium, 5f, Color.DARK_GRAY, layout.englishName(), layout, scaleX, scaleY);
                drawText(g, data.cardNumber(), dotumMedium, 5f, Color.DARK_GRAY, layout.cardNumber(), layout, scaleX, scaleY);
                drawText(g, data.address(), dotumMedium, 4.5f, Color.DARK_GRAY, layout.address(), layout, scaleX, scaleY);
            }
            // 발급일자는 HONOR_CITIZEN은 이름 왼쪽 끝 기준선에 맞춘다(이름도 왼쪽 열에 속함).
            // VISITOR는 이름/영문명이 카드 중앙 정렬이라 이름 기준으로 맞추면 오히려 카드번호/주소
            // 왼쪽 그룹에서 어긋난다(실제 렌더링 후 발견) — 대신 카드번호의 왼쪽 끝을 기준으로 맞춘다
            // (사용자 확인). HONOR_KOREAN은 카드 우측(x=+79.96)에 별도 배치되는 디자인이라 중앙 정렬 유지.
            if (cardType == CardTypeCode.HONOR_CITIZEN) {
                double nameLeftEdge = leftEdgeX(data.fullName(), dotumBold, 8f, layout.name(), layout, scaleX);
                drawTextAtPixelX(g, "발급일자 " + formatIssueDate(data.issueDate()), dotumMedium, 4.5f, Color.DARK_GRAY,
                        nameLeftEdge, layout.issueDate(), layout, scaleX, scaleY);
            } else if (cardType == CardTypeCode.VISITOR) {
                double cardNumberLeftEdge = leftEdgeX(data.cardNumber(), dotumMedium, 5f, layout.cardNumber(), layout, scaleX);
                drawTextAtPixelX(g, "발급일자 " + formatIssueDate(data.issueDate()), dotumMedium, 4.5f, Color.DARK_GRAY,
                        cardNumberLeftEdge, layout.issueDate(), layout, scaleX, scaleY);
            } else {
                drawText(g, "발급일자 " + formatIssueDate(data.issueDate()), dotumMedium, 4.5f, Color.DARK_GRAY,
                        layout.issueDate(), layout, scaleX, scaleY);
            }
            drawZodiac(g, data.zodiacBranch(), data.zodiacDesignSet(), layout.zodiac(), layout, scaleX, scaleY);
            drawSlotImage(g, dir, LOGO_CANDIDATES, data.logo(), layout.issuerLogo(), layout, scaleX, scaleY);
            drawSlotImage(g, dir, SEAL_CANDIDATES, data.seal(), layout.seal(), layout, scaleX, scaleY);
        } finally {
            g.dispose();
        }
        return toPngBytes(bg);
    }

    // 뒷면(한국이름풀이) — 배경 위에 이름·(있으면)한자·영문명·(있으면)한자뜻음·풀이를 얹는다.
    // 배경 자체(뒷면.png)는 신청자 정보와 무관한 디자인 고정 그래픽이라 필드가 없다.
    byte[] composeBack(CardTypeCode cardType, int design, CardMemberData data) {
        if (cardType == CardTypeCode.STUDENT) {
            return composeStudentBack(data);
        }
        CardBackLayout layout = CardLayouts.BACK.get(cardType);
        if (layout == null || isUnverifiedDesign(cardType, design)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String dir = TEMPLATE_ROOT + cardType.name() + "/" + design + "/";
        BufferedImage bg = copy(resolveImage(dir, BACK_CANDIDATES));
        double scaleX = bg.getWidth() / layout.baseWidth();
        double scaleY = bg.getHeight() / layout.baseHeight();
        CardBackVariant variant = data.hasHanja() ? layout.hanjaVariant() : layout.noHanjaVariant();

        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try {
            drawBackText(g, titleFallbackBackText(), batangBold, 8f, Color.BLACK,
                    layout.title(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            drawBackText(g, data.fullName(), dotumBold, 7f, Color.BLACK,
                    variant.name(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            drawBackText(g, data.englishName(), dotumMedium, 4.5f, Color.DARK_GRAY,
                    variant.englishName(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            if (data.hasHanja()) {
                drawBackText(g, "(" + data.chineseName() + ")", batangBold, 5.5f, Color.DARK_GRAY,
                        variant.hanja(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
                drawBackText(g, data.nameMeaning(), dotumMedium, 4f, Color.DARK_GRAY,
                        variant.hanjaMeaning(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            }
            drawBackTextWrapped(g, data.nameInterpretation(), dotumMedium, 4f, Color.DARK_GRAY,
                    variant.interpretation(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY,
                    layout.baseWidth() * INTERPRETATION_WIDTH_RATIO);
        } finally {
            g.dispose();
        }
        return toPngBytes(bg);
    }

    // 4-C: 학생증 앞면 — 다른 3종과 달리 배경(templateFront)이 classpath가 아니라 S3에서 온
    // 원본 바이트다(CardRenderPreparation이 UploadFile 경유로 미리 받아 data에 실어 보낸다, 4-D).
    // 레이아웃도 카드종류가 아니라 orientation으로 고른다(같은 STUDENT 안에서 세로/가로 2세트).
    private byte[] composeStudentFront(CardMemberData data) {
        // Map.of()는 불변 맵이라 get(null)이 null을 반환하지 않고 NPE를 던진다(다른 3종의
        // CardLayouts.FRONT.get(cardType)은 cardType이 항상 non-null이라 안 걸렸던 함정) —
        // studentOrientation null을 먼저 걸러야 아래 .get() 호출이 안전하다.
        if (data.studentOrientation() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        CardStudentFrontLayout layout = CardLayouts.STUDENT_FRONT.get(data.studentOrientation());
        if (layout == null || data.templateFront() == null || data.templateFront().length == 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        BufferedImage bg = copy(readPhoto(data.templateFront()));
        double scaleX = bg.getWidth() / layout.baseWidth();
        double scaleY = bg.getHeight() / layout.baseHeight();

        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        try {
            double bw = layout.baseWidth();
            double bh = layout.baseHeight();
            drawTitleGeneric(g, CardTypeCode.STUDENT, layout.title(), bw, bh, scaleX, scaleY);
            drawStudentPhoto(g, data.photo(), layout, scaleX, scaleY);
            drawTextGeneric(g, data.fullName(), batangBold, 12.11f, Color.BLACK, layout.name(), bw, bh, scaleX, scaleY);
            // 이름/영문명, 학번/학과처럼 짝을 이루는 줄은 각자 표 좌표로 따로 중앙정렬하면 문자열
            // 길이가 다를 때 왼쪽 끝이 어긋난다(탐색 렌더링에서 실측 확인 — 예: "Jo Grinnarae"가
            // "조그린나래"보다 오른쪽으로 밀림). 위 줄의 왼쪽 끝에 맞춰 아래 줄을 왼쪽 정렬한다.
            double nameLeftEdge = leftEdgeXGeneric(data.fullName(), batangBold, 12.11f, layout.name(), bw, scaleX);
            drawTextAtPixelXGeneric(g, data.englishName(), dotumBold, 6.7f, Color.DARK_GRAY, nameLeftEdge,
                    layout.englishName(), bh, scaleX, scaleY);
            if (data.isUniversity()) {
                drawTextGeneric(g, data.studentId(), dotumMedium, 7.6f, Color.BLACK, layout.studentId(), bw, bh, scaleX, scaleY);
                double studentIdLeftEdge = leftEdgeXGeneric(data.studentId(), dotumMedium, 7.6f, layout.studentId(), bw, scaleX);
                drawTextAtPixelXGeneric(g, data.department(), dotumMedium, 7.6f, Color.BLACK, studentIdLeftEdge,
                        layout.department(), bh, scaleX, scaleY);
            } else {
                drawTextGeneric(g, "생년월일 " + formatIssueDate(data.birthDate()), dotumMedium, 7.6f, Color.BLACK,
                        layout.birthDate(), bw, bh, scaleX, scaleY);
            }
            drawTextGeneric(g, "발급일자 " + formatIssueDate(data.issueDate()), dotumBold, 7f, Color.BLACK,
                    layout.issueDate(), bw, bh, scaleX, scaleY);
            drawZodiacGeneric(g, data.zodiacBranch(), data.zodiacDesignSet(), layout.zodiac(), bw, bh, scaleX, scaleY);
        } finally {
            g.dispose();
        }
        return toPngBytes(bg);
    }

    // 4-C: 학생증 뒷면 — 배경도 S3(templateBack). 필드 구성은 이름/한자/영문/풀이 4개뿐이라
    // 한자뜻음 줄이 없는 것 빼고는 다른 3종의 뒷면 합성과 로직이 같다(공용 drawBackText 재사용).
    // 타이틀은 이미지가 아니라 텍스트로 — 다른 3종과 동일한 폰트·크기·색(batangBold/8f/BLACK)으로
    // "한국이름풀이"를 그린다. 원본 시안(학생증_뒷면타이틀.png)도 학생증 뒷면 타이틀이 "한국이름풀이"임을
    // 보여준다 — 이전엔 실수로 앞면용 titleFallbackText(STUDENT)("학 생 증")를 재사용해서 뒷면에도
    // "학생증"이 찍히던 버그였다(실제 렌더링으로 발견, 2026-09-06).
    private byte[] composeStudentBack(CardMemberData data) {
        // composeStudentFront와 동일한 이유(Map.of().get(null) NPE) — null 먼저 거른다.
        if (data.studentOrientation() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        CardBackLayout layout = CardLayouts.STUDENT_BACK.get(data.studentOrientation());
        if (layout == null || data.templateBack() == null || data.templateBack().length == 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        BufferedImage bg = copy(readPhoto(data.templateBack()));
        double scaleX = bg.getWidth() / layout.baseWidth();
        double scaleY = bg.getHeight() / layout.baseHeight();
        CardBackVariant variant = data.hasHanja() ? layout.hanjaVariant() : layout.noHanjaVariant();

        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try {
            drawBackText(g, titleFallbackBackText(), batangBold, 8f, Color.BLACK,
                    layout.title(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            drawBackText(g, data.fullName(), batangBold, 12.11f, Color.BLACK,
                    variant.name(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            drawBackText(g, data.englishName(), batangBold, 9f, Color.DARK_GRAY,
                    variant.englishName(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            if (data.hasHanja()) {
                drawBackText(g, "(" + data.chineseName() + ")", batangBold, 9f, Color.DARK_GRAY,
                        variant.hanja(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
            }
            drawBackTextWrapped(g, data.nameInterpretation(), dotumMedium, 8f, Color.DARK_GRAY,
                    variant.interpretation(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY,
                    layout.baseWidth() * STUDENT_INTERPRETATION_WIDTH_RATIO);
        } finally {
            g.dispose();
        }
        return toPngBytes(bg);
    }

    // 학생증 사진 슬롯 — 다른 3종처럼 디자인별 참고 파일(사진.png)로 크기를 재지 않는다(학생증
    // 디자인엔 그 참고 파일 자체가 없다, 에셋 누락). 대신 레이아웃에 박아둔 고정 크기(잠정값)로
    // cover-fit한다.
    private void drawStudentPhoto(Graphics2D g, byte[] photoBytes, CardStudentFrontLayout layout,
            double scaleX, double scaleY) {
        if (photoBytes == null || photoBytes.length == 0) {
            return;
        }
        int w = (int) Math.round(layout.photoWidth() * scaleX);
        int h = (int) Math.round(layout.photoHeight() * scaleY);
        BufferedImage fitted = coverFit(readPhoto(photoBytes), w, h);
        drawImageCenteredGeneric(g, fitted, layout.photo(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
    }

    // --- 아래는 기존 CardLayout 전용 primitive를 (baseWidth, baseHeight) 원시값 기반으로 일반화한
    // 버전이다. 학생증 레이아웃(CardStudentFrontLayout)이 CardLayout과 다른 레코드 타입이라 기존
    // primitive를 그대로 재사용할 수 없어서 추출했다 — 기존 CardLayout 기반 메서드는 이 일반화
    // 버전에 위임하도록 바꿔서 로직 중복은 없다. drawBackText는 이미 이 형태였다(원래도 baseWidth/
    // baseHeight를 직접 받음).

    private void drawTitleGeneric(Graphics2D g, CardTypeCode cardType, CardFieldOffset offset,
            double baseWidth, double baseHeight, double scaleX, double scaleY) {
        String titlePath = TEMPLATE_ROOT + cardType.name() + "/타이틀.png";
        if (resourceExists(titlePath)) {
            drawImageCenteredGeneric(g, copy(loadImage(titlePath)), offset, baseWidth, baseHeight, scaleX, scaleY);
        } else {
            drawTextGeneric(g, titleFallbackText(cardType), batangBold, 9f, Color.WHITE, offset, baseWidth, baseHeight, scaleX, scaleY);
        }
    }

    private void drawZodiacGeneric(Graphics2D g, String zodiacBranch, int zodiacDesignSet, CardFieldOffset offset,
            double baseWidth, double baseHeight, double scaleX, double scaleY) {
        if (zodiacBranch == null || offset == null) {
            return;
        }
        String path = ZodiacIcon.resourcePathFor(zodiacBranch, zodiacDesignSet);
        if (path == null || !resourceExists(path)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        BufferedImage icon = copy(loadImage(path));
        double scale = (ZODIAC_BASE_WIDTH * scaleX) / icon.getWidth();
        int targetW = (int) Math.round(icon.getWidth() * scale);
        int targetH = (int) Math.round(icon.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(icon, 0, 0, targetW, targetH, null);
        sg.dispose();
        drawImageCenteredGeneric(g, scaled, offset, baseWidth, baseHeight, scaleX, scaleY);
    }

    private void drawImageCenteredGeneric(Graphics2D g, BufferedImage img, CardFieldOffset offset,
            double baseWidth, double baseHeight, double scaleX, double scaleY) {
        double cx = (baseWidth / 2 + offset.x()) * scaleX;
        double cy = (baseHeight / 2 + offset.y()) * scaleY;
        int x = (int) Math.round(cx - img.getWidth() / 2.0);
        int y = (int) Math.round(cy - img.getHeight() / 2.0);
        g.drawImage(img, x, y, null);
    }

    private void drawTextGeneric(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            CardFieldOffset offset, double baseWidth, double baseHeight, double scaleX, double scaleY) {
        if (text == null || text.isBlank() || offset == null) {
            return;
        }
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        g.setColor(color);
        FontRenderContext frc = g.getFontRenderContext();
        TextMetrics metrics = measure(text, font, frc);
        double cx = (baseWidth / 2 + offset.x()) * scaleX;
        double cy = (baseHeight / 2 + offset.y()) * scaleY;
        double x = cx - metrics.width() / 2.0;
        double y = cy + metrics.height() / 2.0 - metrics.descent();
        drawMeasured(g, text, font, frc, (float) x, (float) y);
    }

    private double leftEdgeXGeneric(String text, Font baseFont, float sizeAtBaseScale, CardFieldOffset offset,
            double baseWidth, double scaleX) {
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        FontRenderContext frc = new FontRenderContext(null, true, true);
        double width = measure(text == null ? "" : text, font, frc).width();
        double cx = (baseWidth / 2 + offset.x()) * scaleX;
        return cx - width / 2.0;
    }

    private void drawTextAtPixelXGeneric(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale,
            Color color, double pixelX, CardFieldOffset offset, double baseHeight, double scaleX, double scaleY) {
        if (text == null || text.isBlank() || offset == null) {
            return;
        }
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        g.setColor(color);
        FontRenderContext frc = g.getFontRenderContext();
        TextMetrics metrics = measure(text, font, frc);
        double cy = (baseHeight / 2 + offset.y()) * scaleY;
        double y = cy + metrics.height() / 2.0 - metrics.descent();
        drawMeasured(g, text, font, frc, (float) pixelX, (float) y);
    }

    // 4-E: 문자열 폭/높이/descent — font가 text 전체를 지원하면(canDisplayUpTo == -1) 기존
    // getStringBounds 경로 그대로(호출부 전체의 기존 렌더링 결과를 1픽셀도 안 바꾸기 위해 이 경로는
    // 손대지 않는다). 못 그리는 글자가 하나라도 있을 때만 mixed-font 경로(TextLayout)로 잰다.
    private record TextMetrics(double width, double height, double descent) {
    }

    private TextMetrics measure(String text, Font font, FontRenderContext frc) {
        if (font.canDisplayUpTo(text) == -1) {
            Rectangle2D bounds = font.getStringBounds(text, frc);
            return new TextMetrics(bounds.getWidth(), bounds.getHeight(), font.getLineMetrics(text, frc).getDescent());
        }
        TextLayout layout = buildMixedLayout(text, font, frc);
        return new TextMetrics(layout.getAdvance(), layout.getAscent() + layout.getDescent(), layout.getDescent());
    }

    // measure()와 반드시 같은 분기 조건을 써야 좌표 계산과 실제 그리기가 어긋나지 않는다.
    private void drawMeasured(Graphics2D g, String text, Font font, FontRenderContext frc, float x, float y) {
        if (font.canDisplayUpTo(text) == -1) {
            g.setFont(font);
            g.drawString(text, x, y);
        } else {
            buildMixedLayout(text, font, frc).draw(g, x, y);
        }
    }

    // 주 폰트(font)가 표시 못 하는 글자만 cjkFallback으로 바꿔 그리는 AttributedString을 만든다.
    // 주 폰트가 굵게(Bold)면 fallback도 합성 볼드(deriveFont(BOLD, size))로 맞춰 굵기 차이가 눈에
    // 덜 띄게 한다 — Noto Sans KR은 이 프로젝트엔 Regular 하나만 있어 진짜 Bold 파일이 없다.
    private TextLayout buildMixedLayout(String text, Font font, FontRenderContext frc) {
        AttributedString attributed = new AttributedString(text);
        attributed.addAttribute(TextAttribute.FONT, font);
        Font fallbackStyled = font.isBold()
                ? cjkFallback.deriveFont(Font.BOLD, font.getSize2D())
                : cjkFallback.deriveFont(font.getSize2D());
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int codePointLength = Character.charCount(codePoint);
            if (!font.canDisplay(codePoint)) {
                attributed.addAttribute(TextAttribute.FONT, fallbackStyled, i, i + codePointLength);
            }
            i += codePointLength;
        }
        return new TextLayout(attributed.getIterator(), frc);
    }

    // 세 카드종류 모두 뒷면 타이틀은 "한국이름풀이"로 동일하다(HONOR_KOREAN/HONOR_CITIZEN/VISITOR
    // 시안_최종.jpg 전부 육안 확인). STUDENT는 CardLayouts.BACK에 없어 이 메서드까지 오지 않는다.
    private String titleFallbackBackText() {
        return "한 국 이 름 풀 이";
    }

    // 방문증/1은 앞면.png/뒷면.png가 없고 "대지 1.png" 파일 하나뿐이라 그게 앞면인지 뒷면인지
    // 확인이 안 됐다(TODO.md 참고). FRONT_CANDIDATES에 "대지 1.png"가 있어 이 디자인도 별다른
    // 오류 없이 "성공"해버리므로, 잘못된 이미지로 카드가 만들어지는 걸 막기 위해 명시적으로 막는다.
    private boolean isUnverifiedDesign(CardTypeCode cardType, int design) {
        return cardType == CardTypeCode.VISITOR && design == 1;
    }

    private void drawZodiac(Graphics2D g, String zodiacBranch, int zodiacDesignSet, CardFieldOffset offset,
            CardLayout layout, double scaleX, double scaleY) {
        drawZodiacGeneric(g, zodiacBranch, zodiacDesignSet, offset, layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
    }

    // 로고·직인은 신청자가 업로드한 이미지를 슬롯 크기에 coverFit해서 그린다. bytes가 null이면(정책상
    // 로고·직인이 없는 신청) 그리지 않는다. 슬롯 참고 파일이 이 디자인에 없으면(에셋 누락, 예:
    // HONOR_CITIZEN/2엔 직인.png가 없음) 조용히 건너뛴다 — 렌더링 자체를 막을 이유는 아니다.
    private void drawSlotImage(Graphics2D g, String dir, List<String> candidates, byte[] bytes,
            CardFieldOffset offset, CardLayout layout, double scaleX, double scaleY) {
        if (bytes == null || bytes.length == 0 || offset == null) {
            return;
        }
        BufferedImage slot = resolveOptionalImage(dir, candidates);
        if (slot == null) {
            return;
        }
        BufferedImage fitted = coverFit(readPhoto(bytes), slot.getWidth(), slot.getHeight());
        drawImageCentered(g, fitted, offset, layout, scaleX, scaleY);
    }

    private void drawBackText(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            CardFieldOffset offset, double baseWidth, double baseHeight, double scaleX, double scaleY) {
        if (text == null || text.isBlank() || offset == null) {
            return;
        }
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        g.setColor(color);
        FontRenderContext frc = g.getFontRenderContext();
        TextMetrics metrics = measure(text, font, frc);
        double cx = (baseWidth / 2 + offset.x()) * scaleX;
        double cy = (baseHeight / 2 + offset.y()) * scaleY;
        double x = cx - metrics.width() / 2.0;
        double y = cy + metrics.height() / 2.0 - metrics.descent();
        drawMeasured(g, text, font, frc, (float) x, (float) y);
    }

    // "한국이름풀이"의 뜻풀이(nameInterpretation)는 자유 문장이라 이름·한자 같은 짧은 라벨과 달리
    // 한 줄로 그리면 카드 밖으로 잘려나간다(실제 렌더링으로 발견, 2026-09-05) — 지정한 폭(maxWidth,
    // baseWidth와 같은 단위)을 넘지 않도록 단어 경계에서 줄바꿈하고, 줄 전체 블록을 기존 앵커
    // 좌표(offset)에 세로 중앙 정렬한다(줄 수가 늘어도 텍스트 중심이 디자인 의도한 위치에서 안 벗어남).
    private void drawBackTextWrapped(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            CardFieldOffset offset, double baseWidth, double baseHeight, double scaleX, double scaleY,
            double maxWidthUnits) {
        if (text == null || text.isBlank() || offset == null) {
            return;
        }
        Font font = baseFont.deriveFont((float) (sizeAtBaseScale * scaleX));
        g.setColor(color);
        FontRenderContext frc = g.getFontRenderContext();
        double maxWidthPx = maxWidthUnits * scaleX;
        List<String> lines = wrapByWidth(text, font, frc, maxWidthPx);

        double cx = (baseWidth / 2 + offset.x()) * scaleX;
        double cy = (baseHeight / 2 + offset.y()) * scaleY;
        // 줄 높이는 첫 줄 기준으로 재고(고정폭 아님 — 한글은 줄마다 실측 높이가 사실상 같음), 줄 사이
        // 여백은 가독성을 위해 20% 더한다.
        double lineHeight = measure(lines.get(0), font, frc).height() * 1.2;
        double blockHeight = lineHeight * lines.size();
        double firstBaselineY = cy - blockHeight / 2.0 + lineHeight - lineHeight * 0.2 / 2.0;
        for (int i = 0; i < lines.size(); i++) {
            TextMetrics metrics = measure(lines.get(i), font, frc);
            double x = cx - metrics.width() / 2.0;
            double y = firstBaselineY + lineHeight * i - metrics.descent();
            drawMeasured(g, lines.get(i), font, frc, (float) x, (float) y);
        }
    }

    // 공백 기준 그리디 줄바꿈 — 한 단어가 단독으로 maxWidthPx를 넘으면(매우 긴 합성어 등) 그 단어만
    // 넘치는 채로 한 줄에 둔다(강제 음절 분할은 하지 않음 — 실제 데이터셋 700개 전부 이 케이스 없음).
    private List<String> wrapByWidth(String text, Font font, FontRenderContext frc, double maxWidthPx) {
        List<String> lines = new ArrayList<>();
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.getStringBounds(candidate, frc).getWidth() > maxWidthPx && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private String formatIssueDate(LocalDate issueDate) {
        return issueDate == null ? "" : issueDate.format(ISSUE_DATE_FORMAT);
    }

    private void drawTitle(Graphics2D g, CardTypeCode cardType, CardLayout layout, double scaleX, double scaleY) {
        drawTitleGeneric(g, cardType, layout.title(), layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
    }

    private String titleFallbackText(CardTypeCode cardType) {
        return switch (cardType) {
            case HONOR_KOREAN -> "명 예 한 국 인 증";
            case HONOR_CITIZEN -> "한 국 명 예 시 민 증";
            case VISITOR -> "방 문 증";
            case STUDENT -> "학 생 증";
        };
    }

    private void drawPhoto(Graphics2D g, String dir, byte[] photoBytes, CardLayout layout, double scaleX, double scaleY) {
        if (photoBytes == null || photoBytes.length == 0) {
            return;
        }
        BufferedImage slot = resolveImage(dir, PHOTO_CANDIDATES);
        BufferedImage photo = readPhoto(photoBytes);
        BufferedImage fitted = coverFit(photo, slot.getWidth(), slot.getHeight());
        drawImageCentered(g, fitted, layout.photo(), layout, scaleX, scaleY);
    }

    // 신청자 사진(임의 크기)을 템플릿 사진 자리 크기에 꽉 채워 중앙 크롭한다(cover-fit).
    private BufferedImage coverFit(BufferedImage src, int targetW, int targetH) {
        double scale = Math.max(targetW / (double) src.getWidth(), targetH / (double) src.getHeight());
        int scaledW = (int) Math.ceil(src.getWidth() * scale);
        int scaledH = (int) Math.ceil(src.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, scaledW, scaledH, null);
        g.dispose();
        int x = (scaledW - targetW) / 2;
        int y = (scaledH - targetH) / 2;
        return scaled.getSubimage(Math.max(x, 0), Math.max(y, 0),
                Math.min(targetW, scaledW), Math.min(targetH, scaledH));
    }

    private void drawImageCentered(Graphics2D g, BufferedImage img, CardFieldOffset offset,
            CardLayout layout, double scaleX, double scaleY) {
        drawImageCenteredGeneric(g, img, offset, layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
    }

    private void drawText(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            CardFieldOffset offset, CardLayout layout, double scaleX, double scaleY) {
        drawTextGeneric(g, text, baseFont, sizeAtBaseScale, color, offset, layout.baseWidth(), layout.baseHeight(), scaleX, scaleY);
    }

    // 가운데 정렬(drawText)로 그렸을 때의 왼쪽 끝 픽셀 x를 계산한다 — 짝을 이루는 아래 줄들을 그
    // x에 왼쪽 정렬로 맞추기 위한 기준점. 서로 다른 좌표에서 각자 중앙 정렬하면 문자열 길이가 다를 때
    // 왼쪽 끝이 어긋난다(HONOR_CITIZEN에서 주소·발급일자가 이름보다 오른쪽으로 밀리는 문제, 실제
    // 렌더링 후 발견).
    private double leftEdgeX(String text, Font baseFont, float sizeAtBaseScale, CardFieldOffset offset,
            CardLayout layout, double scaleX) {
        return leftEdgeXGeneric(text, baseFont, sizeAtBaseScale, offset, layout.baseWidth(), scaleX);
    }

    // 짝을 이루는 위 줄(이름)의 왼쪽 끝에 맞춰 왼쪽 정렬로 그린다. y좌표·폰트 크기는 이 필드 자신의
    // 값을 그대로 쓰고, x만 pixelX로 고정한다.
    private void drawTextAtPixelX(Graphics2D g, String text, Font baseFont, float sizeAtBaseScale, Color color,
            double pixelX, CardFieldOffset offset, CardLayout layout, double scaleX, double scaleY) {
        drawTextAtPixelXGeneric(g, text, baseFont, sizeAtBaseScale, color, pixelX, offset, layout.baseHeight(), scaleX, scaleY);
    }

    private BufferedImage resolveImage(String dir, List<String> candidates) {
        BufferedImage image = resolveOptionalImage(dir, candidates);
        if (image == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return image;
    }

    // 슬롯 참고 파일이 이 디자인에 아예 없어도(에셋 누락) 예외를 던지지 않고 null을 돌려준다 —
    // 로고·직인처럼 없어도 렌더링 자체는 계속 가능한 선택적 필드에 쓴다.
    private BufferedImage resolveOptionalImage(String dir, List<String> candidates) {
        for (String candidate : candidates) {
            String path = dir + candidate;
            if (resourceExists(path)) {
                return loadImage(path);
            }
        }
        return null;
    }

    private boolean resourceExists(String classpathPath) {
        return new ClassPathResource(classpathPath).exists();
    }

    // 팔레트(PLTE) PNG를 ImageIO.read()가 이 환경에서 색을 깨뜨려 읽는 문제가 있어(빨강/파랑 반전
    // 도트 패턴), Toolkit 경로(AWT의 다른 디코더)로 읽는다. 캐시는 원본 로딩 결과 — 그리기 전엔
    // 항상 {@link #copy}로 복제해서 캐시 원본이 변형되지 않게 한다.
    private BufferedImage loadImage(String classpathPath) {
        return imageCache.computeIfAbsent(classpathPath, path -> {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                byte[] bytes = in.readAllBytes();
                Image awt = Toolkit.getDefaultToolkit().createImage(bytes);
                MediaTracker tracker = new MediaTracker(new Panel());
                tracker.addImage(awt, 0);
                tracker.waitForID(0);
                BufferedImage buffered = new BufferedImage(
                        awt.getWidth(null), awt.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = buffered.createGraphics();
                g.drawImage(awt, 0, 0, null);
                g.dispose();
                return buffered;
            } catch (IOException | InterruptedException e) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
        });
    }

    private BufferedImage readPhoto(byte[] photoBytes) {
        try {
            Image awt = Toolkit.getDefaultToolkit().createImage(photoBytes);
            MediaTracker tracker = new MediaTracker(new Panel());
            tracker.addImage(awt, 0);
            tracker.waitForID(0);
            BufferedImage buffered = new BufferedImage(
                    awt.getWidth(null), awt.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buffered.createGraphics();
            g.drawImage(awt, 0, 0, null);
            g.dispose();
            return buffered;
        } catch (InterruptedException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private BufferedImage copy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private Font loadFont(String fileName) {
        try (InputStream in = new ClassPathResource(TEMPLATE_ROOT + "fonts/" + fileName).getInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | java.awt.FontFormatException e) {
            throw new IllegalStateException("카드 폰트를 읽을 수 없습니다: " + fileName, e);
        }
    }

    private byte[] toPngBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
