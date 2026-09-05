package com.example.honorcitizen.domain.school;

import java.util.Map;

// 고등학교 마스터 시딩(HighSchoolSeeder)의 동명이교 지역 접두어 변환표. CSV의 정식 시도명(예:
// "서울특별시")을 짧은 표시용 지역명(예: "서울")으로 바꾼다. 전국에 이름이 겹치는 학교가 있을
// 때만 이 접두어를 학교명 앞에 붙인다(HighSchoolSeeder 참고) — 그 외에는 학교명을 원본 그대로 쓴다.
//
// "전남광주통합특별시(광주)"/"(전남)"은 이 CSV 데이터셋에만 존재하는 특이 케이스다(2026-09-05
// 확인 — 이 CSV 기준으로는 광주광역시·전라남도가 하나의 행정구역으로 합쳐져 있고, 시도명 컬럼에
// "전라남도"/"광주광역시"라는 정상 값 자체가 없다). 정책 결정(2026-09-05, 사용자): 이 특이 케이스만
// 아래처럼 flat 매핑을 추가하고, 나머지 17개 정상 시도는 원래 축약표를 그대로 쓴다 — 광주(71건)·
// 전남(143건) 둘 다 내부 동명 중복이 없어(실측 완료) 구/시/군 단위로 더 쪼갤 필요가 없다.
final class RegionAbbreviations {

    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
            Map.entry("서울특별시", "서울"),
            Map.entry("부산광역시", "부산"),
            Map.entry("대구광역시", "대구"),
            Map.entry("인천광역시", "인천"),
            Map.entry("광주광역시", "광주"),
            Map.entry("대전광역시", "대전"),
            Map.entry("울산광역시", "울산"),
            Map.entry("세종특별자치시", "세종"),
            Map.entry("경기도", "경기"),
            Map.entry("강원특별자치도", "강원"),
            Map.entry("충청북도", "충북"),
            Map.entry("충청남도", "충남"),
            Map.entry("전북특별자치도", "전북"),
            Map.entry("전라남도", "전남"),
            Map.entry("경상북도", "경북"),
            Map.entry("경상남도", "경남"),
            Map.entry("제주특별자치도", "제주"),
            // 이 CSV 데이터셋 전용 특이 케이스 — 위 주석 참고.
            Map.entry("전남광주통합특별시(광주)", "광주"),
            Map.entry("전남광주통합특별시(전남)", "전남"));

    private RegionAbbreviations() {
    }

    /**
     * @throws IllegalStateException 매핑표에 없는 시도명 — HighSchoolSeeder가 이미 개교예정/재외한국학교
     *         행을 걸러낸 뒤 부르므로 정상 경로에선 발생하지 않는다. CSV 원본이 바뀌어 새 시도명이
     *         생기면 조용히 잘못된 학교명을 만드는 대신 여기서 바로 실패시킨다.
     */
    static String abbreviate(String sidoName) {
        String abbreviation = ABBREVIATIONS.get(sidoName);
        if (abbreviation == null) {
            throw new IllegalStateException("지역명 매핑표에 없는 시도명입니다: " + sidoName);
        }
        return abbreviation;
    }
}
