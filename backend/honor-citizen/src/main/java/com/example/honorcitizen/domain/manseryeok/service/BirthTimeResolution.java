package com.example.honorcitizen.domain.manseryeok.service;

import java.time.Instant;
import java.util.List;

// BirthTimeZoneResolver.resolve()/confirmOffset()의 결과. status에 따라 어떤 필드가 채워지는지 다르다:
//   EXACT            → selectedOffset, utcInstant 확정, candidates 비어있음
//   NONEXISTENT_LOCAL_TIME / UNKNOWN_TIME → selectedOffset/utcInstant/candidates 전부 비어있음
//   AMBIGUOUS_LOCAL_TIME → candidates에 2개(offset+utcInstant), selectedOffset/utcInstant는 비어있음
public record BirthTimeResolution(
        BirthTimeResolutionStatus status,
        String timezoneId,
        String selectedOffset,
        Instant utcInstant,
        List<OffsetCandidate> candidates) {

    public record OffsetCandidate(String offset, Instant utcInstant) {
    }
}
