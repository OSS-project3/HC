package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.domain.application.entity.NameSelectionStat;
import lombok.Getter;

// 이름별 선택 이력 카운트 — 작명 화면의 "선택 이력 N회" 표시용.
@Getter
public class NameSelectionStatResponse {

    private final String name;
    private final String hanja;
    private final int count;

    private NameSelectionStatResponse(NameSelectionStat stat) {
        this.name = stat.getName();
        this.hanja = stat.getHanja();
        this.count = stat.getSelectedCount();
    }

    public static NameSelectionStatResponse from(NameSelectionStat stat) {
        return new NameSelectionStatResponse(stat);
    }
}
