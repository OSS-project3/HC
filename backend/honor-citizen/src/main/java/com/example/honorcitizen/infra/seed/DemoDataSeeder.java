package com.example.honorcitizen.infra.seed;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.BoardType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.board.entity.Board;
import com.example.honorcitizen.domain.board.repository.BoardRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.review.entity.Review;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 데모/로컬용 시드 데이터(공지·FAQ·후기). `app.seed-demo-data=true`일 때만 활성화된다
 * (docker-compose에서 `APP_SEED_DEMO_DATA=true`로 켬). 프로퍼티가 없으면 빈 자체가 생성되지
 * 않으므로 테스트(@SpringBootTest)에는 영향이 없다. 각 도메인은 비어 있을 때만 채워 idempotent하다.
 * CardTypeSeeder(@Order 없음, 즉 LOWEST_PRECEDENCE) 뒤에 카드종류가 존재하도록 이 시더도
 * LOWEST_PRECEDENCE로 두되, 카드종류가 없으면 후기 시드는 조용히 건너뛴다.
 */
@Component
@Order(2) // CardTypeSeeder(@Order(1)) 뒤에 실행돼 카드종류가 존재하는 상태에서 후기를 시드한다.
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    // 실제 가입과 충돌하지 않는 데모 전용 계정(OAuth 계정이라 비밀번호 로그인 불가). 게시글 작성자·후기 소유자 audit용.
    private static final String DEMO_EMAIL = "seed-demo@hangeul-sejong.local";

    private final BoardRepository boardRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CardTypeRepository cardTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Long demoUserId = ensureDemoUser();
        seedBoards(demoUserId);
        seedReviews(demoUserId);
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
}
