package com.example.honorcitizen.domain.school;

import java.util.ArrayList;
import java.util.List;

// School 마스터 시드 CSV(대학교·고등학교) 공용 파서. 나이스/공공데이터포털 원본 CSV는 영문학교명·
// 주소처럼 콤마가 포함된 값을 큰따옴표로 감싸서 담는다(예: "Dongguk University, Seoul") — naive
// split(",")는 이런 행에서 컬럼이 밀리는 오류가 난다. SchoolSeeder(대학교)에서 먼저 쓰던 로직을
// HighSchoolSeeder(고등학교)도 그대로 재사용하도록 공용 유틸로 뺐다.
final class CsvLineParser {

    private CsvLineParser() {
    }

    static List<String> split(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                columns.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        columns.add(value.toString());
        return columns;
    }
}
