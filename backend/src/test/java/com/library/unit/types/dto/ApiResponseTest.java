package com.library.unit.types.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.types.dto.ApiResponse;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApiResponse} static factory methods.
 */
class ApiResponseTest {

    @Test
    void success_whenGivenData_returns200WithDataAndTimestamp() {
        String payload = "test-data";

        ApiResponse<String> response = ApiResponse.success(payload);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo(payload);
        assertThat(response.getError()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void success_whenGivenNullData_returns200WithNullData() {
        ApiResponse<Void> response = ApiResponse.success(null);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void error_whenGivenStatusCodeAndMessage_returnsErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error(404, "Book not found");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isEqualTo("Book not found");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void error_whenGiven500_returnsInternalServerError() {
        ApiResponse<Void> response = ApiResponse.error(500, "Internal error");

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getError()).isEqualTo("Internal error");
    }
}
