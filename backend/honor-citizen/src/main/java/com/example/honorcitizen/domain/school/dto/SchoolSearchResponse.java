package com.example.honorcitizen.domain.school.dto;

import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.domain.school.entity.School;

public record SchoolSearchResponse(Long id, String name, SchoolType schoolType) {

    public static SchoolSearchResponse from(School school) {
        return new SchoolSearchResponse(school.getId(), school.getName(), school.getSchoolType());
    }
}
