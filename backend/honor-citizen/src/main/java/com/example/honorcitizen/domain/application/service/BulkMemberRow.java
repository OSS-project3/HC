package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.Gender;

import java.time.LocalDate;
import java.time.LocalTime;

record BulkMemberRow(
        String id,
        String englishName,
        LocalDate birthDate,
        String nationality,
        LocalTime birthTime,
        String birthRegion,
        Gender gender,
        LocalDate entryDate,
        String email,
        String phone,
        String address,
        String studentId,
        String department,
        byte[] photoBytes,
        String photoFilename) {
}
