package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.dto.CardDesignResponse;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 2-A/4-B: 관리자가 신청의 카드 종류에 맞는 검수 완료 디자인을 조회한다. 비학생증 카드종류는
// cardTypeId 하나로 목록을 보여주고 관리자가 그중 하나를 골라 cardDesignId로 보낸다. 학생증은
// 디자인이 학교마다 다르고 "관리자가 여러 개 중 선택"하는 구조가 아니라 "신청의 schoolId+
// orientation으로 서버가 자동 확정"하는 구조라(TODO.md "4. 학생증" 정책 3번), applicationId를
// 받아 그 신청의 schoolId+orientation으로 조회 축을 바꾼다 — card-preview/card-generate의
// 요청 계약(cardDesignId 그대로 받음)은 건드리지 않는다, 이 조회 API의 결과에서 그 id를 골라
// 그대로 보내면 된다(학생증은 사실상 0개 또는 1개만 나온다).
@Service
@RequiredArgsConstructor
public class CardDesignService {

    private final CardDesignRepository cardDesignRepository;
    private final CardTypeRepository cardTypeRepository;
    private final ApplicationRepository applicationRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<CardDesignResponse> listCardDesigns(Long adminId, Long cardTypeId, Boolean active, Long applicationId) {
        validateAdmin(adminId);
        CardType cardType = cardTypeRepository.findById(cardTypeId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_TYPE_NOT_FOUND));
        if (cardType.isStudentCard()) {
            return listStudentCardDesigns(applicationId, active);
        }
        List<CardDesign> designs = active == null
                ? cardDesignRepository.findByCardTypeIdOrderByDesignNumber(cardTypeId)
                : cardDesignRepository.findByCardTypeIdAndActiveOrderByDesignNumber(cardTypeId, active);
        return designs.stream().map(CardDesignResponse::from).toList();
    }

    // applicationId가 없으면(비학생증 화면에서 실수로 STUDENT cardTypeId를 넣는 경우 등) 조회할
    // 기준이 없으므로 거절한다. schoolId가 아직 연결 안 된 신청(직접입력, 관리자 학교 연결 전)은
    // "아직 디자인이 없다"는 뜻이라 에러가 아니라 빈 목록을 반환한다(정책 8번).
    private List<CardDesignResponse> listStudentCardDesigns(Long applicationId, Boolean active) {
        if (applicationId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (application.getSchoolId() == null) {
            return List.of();
        }
        // Application.orientation(Orientation)과 CardDesign.orientation(CardDesignOrientation)은
        // 각자 다른 기능에서 독립적으로 추가된 별개 enum이라 타입은 다르지만 값(LANDSCAPE/PORTRAIT)은
        // 같다 — 이름으로 변환한다.
        CardDesignOrientation orientation = CardDesignOrientation.valueOf(application.getOrientation().name());
        List<CardDesign> designs = active == null
                ? cardDesignRepository.findBySchoolIdAndOrientationOrderByDesignNumber(
                        application.getSchoolId(), orientation)
                : cardDesignRepository.findBySchoolIdAndOrientationAndActiveOrderByDesignNumber(
                        application.getSchoolId(), orientation, active);
        return designs.stream().map(CardDesignResponse::from).toList();
    }

    private void validateAdmin(Long adminId) {
        User admin = userService.findById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
