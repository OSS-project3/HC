package com.example.honorcitizen.domain.sajuname;

import com.example.honorcitizen.domain.sajuname.entity.SajuName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SajuNameSeederTest {

    @Test
    void parsesTwoCharacterNameJoiningJawonAndEumWithComma() {
        String json = """
                [{"name":"가헌","hanja":"佳憲","roman":"Ga-heon","jawon":["화","화"],
                  "eum":["목","토"],"reading":"아름다울 가(佳) 법 헌(憲)","meaning":"뜻풀이"}]
                """;

        List<SajuName> result = SajuNameSeeder.parse(toStream(json));

        assertThat(result).hasSize(1);
        SajuName name = result.get(0);
        assertThat(name.getName()).isEqualTo("가헌");
        assertThat(name.getHanja()).isEqualTo("佳憲");
        assertThat(name.getJawon()).isEqualTo("화,화");
        assertThat(name.getEum()).isEqualTo("목,토");
        assertThat(name.getReading()).isEqualTo("아름다울 가(佳) 법 헌(憲)");
        assertThat(name.getMeaning()).isEqualTo("뜻풀이");
    }

    @Test
    void parsesSingleCharacterNameWithLengthOneJawonEum() {
        String json = """
                [{"name":"건","hanja":"建","roman":"Geon","jawon":["토"],
                  "eum":["목"],"reading":"세울 건(建)","meaning":"뜻풀이"}]
                """;

        List<SajuName> result = SajuNameSeeder.parse(toStream(json));

        assertThat(result.get(0).getJawon()).isEqualTo("토");
        assertThat(result.get(0).getEum()).isEqualTo("목");
    }

    private InputStream toStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
