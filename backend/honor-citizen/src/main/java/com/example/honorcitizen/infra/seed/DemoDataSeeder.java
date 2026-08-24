package com.example.honorcitizen.infra.seed;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.EventType;
import com.example.honorcitizen.domain.board.entity.Board;
import com.example.honorcitizen.domain.board.repository.BoardRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.event.entity.EventPost;
import com.example.honorcitizen.domain.event.repository.EventPostRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import com.example.honorcitizen.domain.review.entity.Review;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데모/로컬용 시드 데이터(공지·FAQ·후기). `app.seed-demo-data=true`일 때만 활성화된다
 * (docker-compose에서 `APP_SEED_DEMO_DATA=true`로 켬). 프로퍼티가 없으면 빈 자체가 생성되지
 * 않으므로 테스트(@SpringBootTest)에는 영향이 없다. 각 도메인은 비어 있을 때만 채워 idempotent하다.
 * CardTypeSeeder(@Order 없음, 즉 LOWEST_PRECEDENCE) 뒤에 카드종류가 존재하도록 이 시더도
 * LOWEST_PRECEDENCE로 두되, 카드종류가 없으면 후기 시드는 조용히 건너뛴다.
 */
@Slf4j
@Component
@Order(2) // CardTypeSeeder(@Order(1)) 뒤에 실행돼 카드종류가 존재하는 상태에서 후기를 시드한다.
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    // 실제 가입과 충돌하지 않는 데모 전용 계정(OAuth 계정이라 비밀번호 로그인 불가). 게시글 작성자·후기 소유자 audit용.
    private static final String DEMO_EMAIL = "seed-demo@hangeul-sejong.local";

    // ⚠️ 임시 데모 관리자 계정 — 운영 배포 전 제거. docs/TEMP_ADMIN_LOGIN.md 참고.
    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String ADMIN_PASSWORD = "admin1234!";

    private static final String BOOTH_TEXT =
            "부스를 찾은 방문객에게 한글 오행으로 지은 한국 이름과 카드를 현장에서 제작해 전달했습니다. 참가자와 함께한 인증 사진과 현장 후기를 이곳에 기록으로 남깁니다.";

    private final BoardRepository boardRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CardTypeRepository cardTypeRepository;
    private final EventPostRepository eventPostRepository;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Long demoUserId = ensureDemoUser();
        ensureAdminUser();
        seedBoards(demoUserId);
        seedReviews(demoUserId);
        seedEvents();
    }

    // ⚠️ 임시 데모 관리자 — admin@test.com / admin1234! 로 실제 로그인(ADMIN 권한) 가능하게 시드한다.
    // 존재하지 않을 때만 생성해 idempotent하다. 운영 배포 전 제거할 것(docs/TEMP_ADMIN_LOGIN.md).
    private void ensureAdminUser() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            return;
        }
        User admin = User.createLocalUser(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), "관리자", "010-0000-0000");
        admin.promoteToAdmin();
        userRepository.save(admin);
        log.warn("임시 데모 관리자 계정 시드됨: {} (운영 전 제거 필요)", ADMIN_EMAIL);
    }

    private Long ensureDemoUser() {
        return userRepository.findByEmail(DEMO_EMAIL)
                .orElseGet(() -> userRepository.save(
                        User.createOAuthUser(DEMO_EMAIL, "seed-demo-oauth-id", "seed", "한글과 세종")))
                .getId();
    }

    // 공지사항/FAQ — frontend SupportPage 목데이터(notices/faqs)를 그대로 옮겼다.
    private void seedBoards(Long userId) {
        if (boardRepository.count() > 0) {
            return;
        }
        List<Board> boards = List.of(
                // 공지사항
                Board.create(BoardType.NOTICE, "단체 제작 신청 안내",
                        "단체 카드 제작을 신청하실 경우 제작 신청 페이지에서 법인·단체 신청을 선택해 주세요. 담당자 확인 후 제작 일정과 필요한 자료를 안내드립니다.", userId),
                Board.create(BoardType.NOTICE, "신청양식 다운로드",
                        "단체 카드(명예 한국인증, 명예 시민증, 학생증, 방문증) 신청 시 필요한 신청양식입니다.\n아래 첨부파일을 다운로드하여 작성 후 신청 시 함께 첨부해 주세요.", userId),
                Board.create(BoardType.NOTICE, "모바일·실물 카드 수령 안내",
                        "모바일 카드는 발급 완료 후 신청 조회 페이지에서 확인할 수 있습니다. 실물 카드를 함께 신청하신 경우 등록한 수령지로 순차 배송됩니다.", userId),
                Board.create(BoardType.NOTICE, "공식 서비스 및 운영 안내",
                        "한글과 세종 공식 서비스 운영 안내입니다. 서비스 이용과 제작 신청에 관한 문의는 고객지원 상담·문의 메뉴를 이용해 주세요.", userId),
                // FAQ — title=질문, content=답변
                Board.create(BoardType.FAQ, "제작된 카드는 실제 신분증으로 사용할 수 있나요?",
                        "아니요.\n본 상품은 신분증으로서의 법적 효력을 갖지 않습니다.", userId),
                Board.create(BoardType.FAQ, "작명 의뢰는 각 개인이 직접 하여야 하나요?",
                        "개인 신청과 단체 신청 모두 가능하며, 신청 유형에 맞는 정보를 입력해 주시면 됩니다.", userId),
                Board.create(BoardType.FAQ, "의뢰 후 제작 기간은 어느정도 걸리나요?",
                        "상담과 자료 확인이 완료된 뒤 제작 일정과 수령 방법을 개별 안내드립니다.", userId),
                Board.create(BoardType.FAQ, "원하는 카드 디자인이나 캐릭터를 선택할 수 있나요?",
                        "카드 디자인과 캐릭터는 기본적으로 랜덤으로 발송되지만, 특정 디자인이나 캐릭터를 원하시는 경우, 별도로 문의해 주시면 확인 후 원하시는 디자인이나 캐릭터로 발송해 드립니다.", userId),
                Board.create(BoardType.FAQ, "소량 제작도 가능한가요?",
                        "가능합니다. 수량과 제작 사양에 따라 상세 견적을 안내드립니다.", userId),
                Board.create(BoardType.FAQ, "신규 발급 뿐 아니라 재발급도 가능한가요?",
                        "가능합니다. 기존 신청 정보 확인 후 재발급 절차를 안내드립니다.", userId),
                Board.create(BoardType.FAQ, "카드 발급에 필요한 자료는 무엇인가요?",
                        "카드 유형에 따라 신청자 정보와 사진 등 필요한 자료가 달라집니다. 제작 신청 화면에서 확인해 주세요.", userId),
                Board.create(BoardType.FAQ, "전국 기관을 대상으로 업무가 가능한가요?",
                        "네. 전국 기관 및 단체를 대상으로 상담과 제작 업무를 진행할 수 있습니다.", userId));
        boardRepository.saveAll(boards);
    }

    // 후기 — 프론트에 목데이터가 없어 카드종류별 대표 샘플을 생성한다(실제 사용자 후기로 대체될 때까지 페이지가 비지 않도록).
    private void seedReviews(Long userId) {
        if (reviewRepository.count() > 0) {
            return;
        }
        Map<CardTypeCode, Long> idByCode = new EnumMap<>(CardTypeCode.class);
        for (CardType cardType : cardTypeRepository.findAll()) {
            idByCode.put(cardType.getCode(), cardType.getId());
        }
        if (idByCode.isEmpty()) {
            return; // 카드종류가 아직 시드되지 않았으면 후기 시드는 건너뛴다.
        }
        saveReview(userId, "홍길동", "한국에서의 추억이 이름과 카드로 남았어요", ApplicationType.INDIVIDUAL,
                idByCode.get(CardTypeCode.HONOR_KOREAN), "이름의 뜻을 함께 설명해 주셔서 여행이 끝난 뒤에도 특별한 기억으로 간직하고 있습니다.");
        saveReview(userId, "이소연", "행사 참가자에게 색다른 경험을 선물했습니다", ApplicationType.GROUP,
                idByCode.get(CardTypeCode.HONOR_CITIZEN), "단체로 신청했는데 진행이 매끄럽고 카드 완성도도 높아 참가자 반응이 정말 좋았어요.");
        saveReview(userId, "Michael", "제 한국 이름이 생겨서 뿌듯해요", ApplicationType.INDIVIDUAL,
                idByCode.get(CardTypeCode.HONOR_KOREAN), "발음도 예쁘고 의미도 좋은 이름을 지어주셔서 감사합니다. 카드도 소장 가치가 있어요.");
        saveReview(userId, "박지호", "학생증 디자인이 깔끔해서 만족합니다", ApplicationType.INDIVIDUAL,
                idByCode.get(CardTypeCode.STUDENT), "학교 로고와 함께 제작되어 실제 학생증 같은 느낌이라 기념으로 아주 좋습니다.");
        saveReview(userId, "정은성", "방문 기념으로 딱이에요", ApplicationType.INDIVIDUAL,
                idByCode.get(CardTypeCode.VISITOR), "짧은 방문이었지만 한국 이름이 적힌 방문증을 받아 오래 기억에 남을 것 같습니다.");
        saveReview(userId, "Sophie", "회사 글로벌 행사에서 큰 호응을 얻었어요", ApplicationType.GROUP,
                idByCode.get(CardTypeCode.HONOR_KOREAN), "해외 임직원들에게 한글 이름 카드를 선물했더니 모두 특별한 경험이라며 좋아했습니다.");
    }

    private void saveReview(Long userId, String author, String title, ApplicationType type, Long cardTypeId, String content) {
        if (cardTypeId == null) {
            return;
        }
        reviewRepository.save(Review.create(userId, author, title, type, cardTypeId, content, null));
    }

    // 행사사업(부스 운영/법인·단체 협업) — frontend eventFeedPosts.ts의 boothPosts/collabPosts 목데이터를 이관.
    // 이미지(썸네일·로고)는 프론트 정적 자산을 backend resources/seed/{events,logos}로 옮겨와 시드 시 S3에
    // 업로드하고 그 key를 EventPost에 저장한다 — EventService가 이 key로 presigned URL을 만들어 서빙한다.
    // 정렬은 displayOrder ASC이므로 목데이터 순서대로 0..N을 부여한다.
    private void seedEvents() {
        if (eventPostRepository.count() > 0) {
            return;
        }
        // 같은 이미지 파일이 여러 행에 재사용되므로(예: collaboration-5..8) 리소스 경로별로 업로드 결과를 캐시해
        // S3 중복 업로드를 피한다. 값은 저장할 S3 key(업로드 실패 시 null).
        Map<String, String> uploadedByResource = new HashMap<>();

        // {일자표시, 제목, 장소, 주최, 발급카드, 썸네일파일}
        String[][] booth = {
                {"2026. 12", "서울공예트렌드페어", "서울 코엑스 Hall C", "(재)한국공예·디자인문화진흥원", "명예한국인증 · 방문증", "booth-hero.webp"},
                {"2026. 10", "한국전통문화박람회", "경주 화백컨벤션센터", "문화체육관광부", "명예한국인증 · 학생증", "booth-calligraphy.webp"},
                {"2026. 08", "한글주간 문화행사", "국립한글박물관", "국립한글박물관", "방문증", "booth-display.webp"},
                {"2026. 06", "부산국제관광전", "부산 벡스코", "부산광역시", "방문증", "booth-card-delivery.webp"},
                {"2026. 04", "외국인 유학생 문화 교류전", "서울글로벌센터", "서울글로벌센터", "학생증 · 명예시민증", "collaboration-1.webp"},
                {"2026. 03", "K-컬처 관광 설명회", "인천 송도컨벤시아", "한국관광공사", "방문증", "collaboration-2.webp"},
                {"2026. 02", "세계 한글 체험 부스", "부산 문화회관", "부산문화재단", "명예한국인증", "collaboration-3.webp"},
                {"2026. 01", "국제 교류 환영 행사", "제주국제컨벤션센터", "제주특별자치도", "명예시민증 · 방문증", "collaboration-4.webp"},
        };
        for (int i = 0; i < booth.length; i++) {
            String[] r = booth[i];
            String thumbnailPath = uploadThumbnail(r[5], uploadedByResource);
            eventPostRepository.save(EventPost.create(EventType.BOOTH, r[1], null, r[0], r[2], r[3], r[4],
                    BOOTH_TEXT, thumbnailPath, null, null, true, i));
        }

        // {일자표시, 제목, 장소, 주최, 회사명, 발급카드, 내용, 썸네일파일, 로고파일}
        String[][] collab = {
                {"2027.01.15", "삼성 글로벌 임직원 한국 이름 체험", "삼성전자 글로벌 캠퍼스", "Samsung", "Samsung", "명예한국인증 · 방문증", "해외 임직원 온보딩 행사에 한글 이름 추천과 명예한국인증 발급 체험을 연계했습니다.", "corporate-samsung.webp", "samsung-wordmark.svg"},
                {"2027.01.08", "네이버 글로벌 파트너 문화 프로그램", "네이버 1784", "NAVER", "NAVER", "명예시민증", "글로벌 파트너 방문 일정에 맞춰 한국 이름 카드와 디지털 기념 콘텐츠를 제공했습니다.", "corporate-naver.webp", "naver-wordmark.svg"},
                {"2026.12.22", "현대 모빌리티 초청 고객 행사", "현대 모터스튜디오", "Hyundai", "Hyundai", "방문증", "해외 초청 고객에게 한글 이름 방문증을 발급하고 브랜드 투어 경험과 연결했습니다.", "corporate-hyundai.webp", "hyundai-wordmark.svg"},
                {"2026.12.12", "카카오 외국인 크리에이터 밋업", "카카오 판교 아지트", "Kakao", "Kakao", "명예한국인증", "콘텐츠 크리에이터 교류 행사에서 한글 이름 카드 제작 부스를 운영했습니다.", "corporate-kakao.webp", "kakao-wordmark.svg"},
                {"2026.11.26", "LG 글로벌 고객 초청 문화 체험", "LG 사이언스파크", "LG", "LG", "명예시민증", "글로벌 고객 초청 행사에 한국 이름 풀이와 카드 수령 경험을 더했습니다.", "collaboration-5.webp", "lg-wordmark.svg"},
                {"2026.11.18", "기아 해외 딜러 네트워크 교류회", "기아 브랜드 체험관", "Kia", "Kia", "방문증", "해외 딜러 초청 프로그램에서 참가자별 한글 이름 방문증을 제작했습니다.", "collaboration-6.webp", "kia-wordmark.svg"},
                {"2026.10.30", "라인 글로벌 팀 문화 교류 행사", "라인 오피스 라운지", "LINE", "LINE", "학생증 · 방문증", "다국적 팀 교류 행사에서 한글 이름 카드와 팀별 기념 촬영을 연계했습니다.", "collaboration-7.webp", "line.svg"},
                {"2026.10.14", "구글 스타트업 캠퍼스 파트너 데이", "스타트업 캠퍼스", "Google", "Google", "명예한국인증", "해외 창업가 네트워킹 행사에 한국 이름 추천 카드 체험을 구성했습니다.", "collaboration-8.webp", "google-wordmark.svg"},
                {"2026.09.28", "삼성 글로벌 협력사 초청 데이", "삼성 디지털시티", "Samsung", "Samsung", "방문증", "해외 협력사 방문 일정에 맞춰 한글 이름 방문증과 현장 기념 촬영 프로그램을 운영했습니다.", "collaboration-1.webp", "samsung.svg"},
                {"2026.09.12", "네이버 해외 인턴 문화 온보딩", "네이버 1784", "NAVER", "NAVER", "학생증 · 명예시민증", "해외 인턴 참가자를 대상으로 한국 이름 추천과 디지털 카드 발급 체험을 제공했습니다.", "collaboration-2.webp", "naver.svg"},
                {"2026.08.26", "카카오 글로벌 파트너 교류회", "카카오 판교 아지트", "Kakao", "Kakao", "명예한국인증", "파트너 교류 행사에서 참가자별 한글 이름 카드와 협업 기록 콘텐츠를 함께 제작했습니다.", "collaboration-3.webp", "kakao.svg"},
                {"2026.08.09", "LG 해외 연구원 환영 프로그램", "LG 사이언스파크", "LG", "LG", "방문증", "해외 연구원 방문 프로그램에 한국 이름 풀이와 방문증 수령 경험을 더했습니다.", "collaboration-4.webp", "lg.svg"},
                {"2026.07.24", "현대 글로벌 고객 브랜드 투어", "현대 모터스튜디오", "Hyundai", "Hyundai", "명예시민증", "브랜드 투어 참가 고객에게 한글 이름 카드와 맞춤형 기념 이미지를 제공했습니다.", "collaboration-5.webp", "hyundai.svg"},
                {"2026.07.10", "기아 글로벌 트레이닝 캠프", "기아 오토랜드", "Kia", "Kia", "학생증 · 방문증", "해외 교육 참가자에게 프로그램 전용 한글 이름 학생증과 방문증을 발급했습니다.", "collaboration-6.webp", "kia.svg"},
                {"2026.06.21", "라인 다국적 팀 리더 워크숍", "라인 오피스 라운지", "LINE", "LINE", "명예한국인증", "다국적 팀 리더 워크숍에 한글 이름 체험과 팀별 인증 카드 제작을 연계했습니다.", "collaboration-7.webp", "line.svg"},
                {"2026.06.06", "구글 글로벌 스타트업 밋업", "스타트업 캠퍼스", "Google", "Google", "방문증", "해외 스타트업 참가자에게 한국 이름 방문증과 네트워킹용 디지털 콘텐츠를 제공했습니다.", "collaboration-8.webp", "google.svg"},
        };
        for (int i = 0; i < collab.length; i++) {
            String[] r = collab[i];
            String thumbnailPath = uploadThumbnail(r[7], uploadedByResource);
            String logoPath = uploadLogo(r[8], uploadedByResource);
            eventPostRepository.save(EventPost.create(EventType.COLLABORATION, r[1], null, r[0], r[2], r[3], r[5],
                    r[6], thumbnailPath, r[4], logoPath, true, i));
        }
    }

    private String uploadThumbnail(String filename, Map<String, String> uploadedByResource) {
        return uploadSeedImage("seed/events/" + filename, "events/thumbnails/seed-" + filename, uploadedByResource);
    }

    private String uploadLogo(String filename, Map<String, String> uploadedByResource) {
        return uploadSeedImage("seed/logos/" + filename, "events/logos/seed-" + filename, uploadedByResource);
    }

    // 클래스패스의 데모 이미지를 S3에 업로드하고 저장 key를 돌려준다. 리소스 누락이나 S3 오류가 나도
    // 시드 전체(및 앱 기동)를 실패시키지 않도록 null로 폴백한다 — 이미지 없이 텍스트만 노출된다.
    // computeIfAbsent는 실패(null)를 캐시에 남기지 않으므로 같은 파일의 다음 참조 때 재시도된다.
    private String uploadSeedImage(String resourcePath, String key, Map<String, String> uploadedByResource) {
        return uploadedByResource.computeIfAbsent(resourcePath, path -> {
            try (var in = new ClassPathResource(path).getInputStream()) {
                storageService.uploadBytes(key, in.readAllBytes(), contentTypeOf(path));
                return key;
            } catch (IOException | RuntimeException e) {
                log.warn("데모 이미지 시드 실패(무시하고 진행). resource={}", path, e);
                return null;
            }
        });
    }

    private String contentTypeOf(String path) {
        return path.endsWith(".svg") ? "image/svg+xml" : "image/webp";
    }
}
