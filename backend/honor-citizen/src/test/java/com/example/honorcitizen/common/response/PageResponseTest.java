package com.example.honorcitizen.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void fromMapsEntityPageToResponsePageWithSamePagingMetadata() {
        var entityPage = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(0, 9), 21);

        PageResponse<String> response = PageResponse.from(entityPage, i -> "item-" + i);

        assertThat(response.getContent()).containsExactly("item-1", "item-2", "item-3");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(9);
        assertThat(response.getTotalElements()).isEqualTo(21);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }
}
