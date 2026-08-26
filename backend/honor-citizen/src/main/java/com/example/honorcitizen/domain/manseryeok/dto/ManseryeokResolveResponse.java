package com.example.honorcitizen.domain.manseryeok.dto;

import com.example.honorcitizen.domain.manseryeok.service.BirthTimeResolution;
import com.example.honorcitizen.domain.manseryeok.service.BirthTimeResolutionStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class ManseryeokResolveResponse {

    private final BirthTimeResolutionStatus status;
    private final String timezoneId;
    private final Double longitude;
    private final String selectedOffset;
    private final Instant utcInstant;
    private final List<OffsetCandidate> candidates;

    private ManseryeokResolveResponse(BirthTimeResolutionStatus status, String timezoneId, Double longitude,
            String selectedOffset, Instant utcInstant, List<OffsetCandidate> candidates) {
        this.status = status;
        this.timezoneId = timezoneId;
        this.longitude = longitude;
        this.selectedOffset = selectedOffset;
        this.utcInstant = utcInstant;
        this.candidates = candidates;
    }

    public static ManseryeokResolveResponse of(BirthTimeResolution resolution, Double longitude) {
        List<OffsetCandidate> candidates = resolution.candidates().stream()
                .map(c -> new OffsetCandidate(c.offset(), c.utcInstant()))
                .toList();
        return new ManseryeokResolveResponse(resolution.status(), resolution.timezoneId(), longitude,
                resolution.selectedOffset(), resolution.utcInstant(), candidates);
    }

    public record OffsetCandidate(String offset, Instant utcInstant) {
    }
}
