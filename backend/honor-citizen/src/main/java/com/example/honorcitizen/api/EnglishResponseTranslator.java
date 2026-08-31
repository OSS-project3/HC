package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationDetailResponse;
import com.example.honorcitizen.domain.board.dto.BoardDetailResponse;
import com.example.honorcitizen.domain.board.dto.BoardListItemResponse;
import com.example.honorcitizen.domain.event.dto.EventDetailResponse;
import com.example.honorcitizen.domain.event.dto.EventListItemResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryDetailResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryListItemResponse;
import com.example.honorcitizen.domain.review.dto.ReviewDetailResponse;
import com.example.honorcitizen.domain.review.dto.ReviewListItemResponse;
import com.example.honorcitizen.infra.translation.AcceptLanguages;
import com.example.honorcitizen.infra.translation.ContentTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Accept-Language 기본 태그가 en일 때, 사용자 노출 자유 텍스트를 영어로 번역한 응답 사본을 만든다.
 * 그 외 언어(ko 포함)면 입력을 그대로 반환한다.
 *
 * 목록 응답은 전체 문자열을 모아 {@link ContentTranslationService#toEnglish(List)} 1회로 배치
 * 번역한다. 번역 대상 필드는 각 DTO의 {@code withTranslated(...)}가 정의하며, 사람 이름·고유명사·
 * 한글값 enum(프론트 계약)·파일명은 번역하지 않는다. 관리자(/api/admin/**) 응답에는 쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class EnglishResponseTranslator {

    private final ContentTranslationService contentTranslationService;

    public PageResponse<BoardListItemResponse> translateBoards(
            PageResponse<BoardListItemResponse> page, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage)) {
            return page;
        }
        List<BoardListItemResponse> items = page.getContent();
        List<String> texts = new ArrayList<>(items.size() * 2);
        for (BoardListItemResponse item : items) {
            texts.add(item.getTitle());
            texts.add(item.getContent());
        }
        List<String> translated = contentTranslationService.toEnglish(texts);
        List<BoardListItemResponse> content = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            content.add(items.get(i).withTranslated(translated.get(i * 2), translated.get(i * 2 + 1)));
        }
        return page.withContent(content);
    }

    public BoardDetailResponse translateBoard(BoardDetailResponse detail, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage)) {
            return detail;
        }
        String nextTitle = detail.getNext() == null ? null : detail.getNext().getTitle();
        List<String> translated = contentTranslationService.toEnglish(
                Arrays.asList(detail.getTitle(), detail.getContent(), nextTitle));
        return detail.withTranslated(translated.get(0), translated.get(1), translated.get(2));
    }

    public PageResponse<ReviewListItemResponse> translateReviews(
            PageResponse<ReviewListItemResponse> page, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage)) {
            return page;
        }
        List<ReviewListItemResponse> items = page.getContent();
        List<String> texts = new ArrayList<>(items.size() * 2);
        for (ReviewListItemResponse item : items) {
            texts.add(item.getTitle());
            texts.add(item.getContent());
        }
        List<String> translated = contentTranslationService.toEnglish(texts);
        List<ReviewListItemResponse> content = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            content.add(items.get(i).withTranslated(translated.get(i * 2), translated.get(i * 2 + 1)));
        }
        return page.withContent(content);
    }

    public ReviewDetailResponse translateReview(ReviewDetailResponse detail, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage)) {
            return detail;
        }
        String nextTitle = detail.getNext() == null ? null : detail.getNext().getTitle();
        List<String> translated = contentTranslationService.toEnglish(
                Arrays.asList(detail.getTitle(), detail.getContent(), nextTitle));
        return detail.withTranslated(translated.get(0), translated.get(1), translated.get(2));
    }

    public PageResponse<EventListItemResponse> translateEvents(
            PageResponse<EventListItemResponse> page, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage)) {
            return page;
        }
        List<EventListItemResponse> items = page.getContent();
        List<String> texts = new ArrayList<>(items.size() * 5);
        for (EventListItemResponse item : items) {
            texts.add(item.getTitle());
            texts.add(item.getEventDateText());
            texts.add(item.getPlace());
            texts.add(item.getHost());
            texts.add(item.getContent());
        }
        List<String> translated = contentTranslationService.toEnglish(texts);
        List<EventListItemResponse> content = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            int base = i * 5;
            content.add(items.get(i).withTranslated(translated.get(base), translated.get(base + 1),
                    translated.get(base + 2), translated.get(base + 3), translated.get(base + 4)));
        }
        return page.withContent(content);
    }

    public EventDetailResponse translateEvent(EventDetailResponse detail, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage)) {
            return detail;
        }
        List<String> translated = contentTranslationService.toEnglish(Arrays.asList(
                detail.getTitle(), detail.getEventDateText(), detail.getPlace(), detail.getHost(),
                detail.getContent()));
        return detail.withTranslated(translated.get(0), translated.get(1), translated.get(2),
                translated.get(3), translated.get(4));
    }

    public List<InquiryListItemResponse> translateInquiries(
            List<InquiryListItemResponse> items, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage)) {
            return items;
        }
        List<String> titles = new ArrayList<>(items.size());
        for (InquiryListItemResponse item : items) {
            titles.add(item.getTitle());
        }
        List<String> translated = contentTranslationService.toEnglish(titles);
        List<InquiryListItemResponse> content = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            content.add(items.get(i).withTranslated(translated.get(i)));
        }
        return content;
    }

    public InquiryDetailResponse translateInquiry(InquiryDetailResponse detail, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage)) {
            return detail;
        }
        List<String> translated = contentTranslationService.toEnglish(
                Arrays.asList(detail.getTitle(), detail.getContent(), detail.getAnswer()));
        return detail.withTranslated(translated.get(0), translated.get(1), translated.get(2));
    }

    public MyApplicationDetailResponse translateMyApplication(
            MyApplicationDetailResponse detail, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage) || detail.getPhotoRejectReason() == null) {
            return detail;
        }
        return detail.withTranslated(contentTranslationService.toEnglish(detail.getPhotoRejectReason()));
    }

    public ApplicationLookupResponse translateLookup(ApplicationLookupResponse response, String acceptLanguage) {
        if (!AcceptLanguages.wantsEnglish(acceptLanguage) || response.getPhotoRejectReason() == null) {
            return response;
        }
        return response.withTranslated(contentTranslationService.toEnglish(response.getPhotoRejectReason()));
    }
}
