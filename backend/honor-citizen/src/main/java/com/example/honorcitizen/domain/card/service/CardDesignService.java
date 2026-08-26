package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
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

// 2-A: 관리자가 신청의 카드 종류에 맞는 검수 완료 디자인을 조회한다. 학생증은 이번 카드 제작 계획의
// 범위 밖이라(TODO.md "관리자 작명 확정·카드 제작 구현 계획" 범위 제외) 조회 자체를 거절한다.
@Service
@RequiredArgsConstructor
public class CardDesignService {

    private final CardDesignRepository cardDesignRepository;
    private final CardTypeRepository cardTypeRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<CardDesignResponse> listCardDesigns(Long adminId, Long cardTypeId, Boolean active) {
        validateAdmin(adminId);
        CardType cardType = cardTypeRepository.findById(cardTypeId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_TYPE_NOT_FOUND));
        if (cardType.isStudentCard()) {
            throw new CustomException(ErrorCode.UNSUPPORTED_CARD_TYPE);
        }
        List<CardDesign> designs = active == null
                ? cardDesignRepository.findByCardTypeIdOrderByDesignNumber(cardTypeId)
                : cardDesignRepository.findByCardTypeIdAndActiveOrderByDesignNumber(cardTypeId, active);
        return designs.stream().map(CardDesignResponse::from).toList();
    }

    private void validateAdmin(Long adminId) {
        User admin = userService.findById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
