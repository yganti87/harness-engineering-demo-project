package com.library.unit.types.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.types.dto.PagedResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for {@link PagedResponse#of}.
 */
class PagedResponseTest {

    @Test
    void of_whenGivenPage_mapsAllFieldsCorrectly() {
        List<String> content = List.of("a", "b", "c");
        PageImpl<String> page = new PageImpl<>(
            content,
            PageRequest.of(1, 5),
            23
        );

        PagedResponse<String> result = PagedResponse.of(page);

        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(23);
        assertThat(result.getTotalPages()).isEqualTo(5);
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void of_whenLastPage_setsLastTrue() {
        PageImpl<String> page = new PageImpl<>(
            List.of("x"),
            PageRequest.of(2, 10),
            21
        );

        PagedResponse<String> result = PagedResponse.of(page);

        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void of_whenEmptyPage_returnsEmptyContent() {
        PageImpl<String> page = new PageImpl<>(
            List.of(),
            PageRequest.of(0, 20),
            0
        );

        PagedResponse<String> result = PagedResponse.of(page);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.isLast()).isTrue();
    }
}
