package com.example.honorcitizen.infra.translation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptLanguagesTest {

    @Test
    void primaryEnglishTagWantsEnglish() {
        assertThat(AcceptLanguages.wantsEnglish("en")).isTrue();
        assertThat(AcceptLanguages.wantsEnglish("en-US")).isTrue();
        assertThat(AcceptLanguages.wantsEnglish("EN-GB")).isTrue();
        assertThat(AcceptLanguages.wantsEnglish("en-US,en;q=0.9,ko;q=0.8")).isTrue();
    }

    @Test
    void koreanOrMissingHeaderKeepsKorean() {
        assertThat(AcceptLanguages.wantsEnglish("ko")).isFalse();
        assertThat(AcceptLanguages.wantsEnglish("ko-KR,ko;q=0.9,en;q=0.8")).isFalse();
        assertThat(AcceptLanguages.wantsEnglish(null)).isFalse();
        assertThat(AcceptLanguages.wantsEnglish("")).isFalse();
        assertThat(AcceptLanguages.wantsEnglish("*")).isFalse();
        assertThat(AcceptLanguages.wantsEnglish("ja")).isFalse();
    }
}
